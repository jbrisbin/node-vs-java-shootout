package shootout

import java.nio.ByteBuffer
import java.nio.channels.AsynchronousFileChannel
import java.nio.channels.CompletionHandler
import java.nio.file.OpenOption
import java.nio.file.Paths
import java.nio.file.StandardOpenOption
import java.security.MessageDigest
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicLong
import org.apache.commons.codec.binary.Base64
import org.codehaus.jackson.map.ObjectMapper
import org.glassfish.grizzly.Grizzly
import org.glassfish.grizzly.http.Method
import org.glassfish.grizzly.http.server.HttpHandler
import org.glassfish.grizzly.http.server.Request
import org.glassfish.grizzly.http.server.Response
import org.glassfish.grizzly.http.server.io.ReadHandler
import org.glassfish.grizzly.memory.ByteBufferManager
import org.slf4j.LoggerFactory

class UploadHandler extends HttpHandler {

	def log = LoggerFactory.getLogger(getClass())
	def mapper = new ObjectMapper()
	def BUFFER_SIZE = 512000
	def headerProcessed = Grizzly.DEFAULT_ATTRIBUTE_BUILDER.createAttribute("header-processed")
	def activeChannel = Grizzly.DEFAULT_ATTRIBUTE_BUILDER.createAttribute("active-channel")
	def md5hash = Grizzly.DEFAULT_ATTRIBUTE_BUILDER.createAttribute("md5-hash")
	def bytesRead = Grizzly.DEFAULT_ATTRIBUTE_BUILDER.createAttribute("bytes-read")
	def completionHandler = Grizzly.DEFAULT_ATTRIBUTE_BUILDER.createAttribute("completion-handler")
	def bytesUploaded = new AtomicLong(0)
	def workerPool = Executors.newSingleThreadExecutor()
	def byteBufferManager = new ByteBufferManager(true, BUFFER_SIZE, 16384)

	@Override void service(final Request request, final Response response) {
		// Immediately suspend the response
		response.suspend()
		// Attributes for this request conversation
		def attrs = request.context.attributes

		switch (request.method) {
			case Method.GET:
				response.status = 200
				response.contentType = "text/plain"
				response.writer.write("total uploaded bytes: " + bytesUploaded.get())
				response.resume()
				break
			case Method.PUT:
				// Output headers only the first time through
				if (!headerProcessed.get(attrs)) {
					response.status = 200
					response.contentType = "application/json"
					headerProcessed.set(attrs, true)
				}
				// Use Java 7's AsynchronousFileChannel to stream data to
				def channel = activeChannel.get(attrs)
				if (!channel) {
					def opts = [
							StandardOpenOption.CREATE,
							StandardOpenOption.TRUNCATE_EXISTING,
							StandardOpenOption.WRITE
					] as Set<OpenOption>
					channel = AsynchronousFileChannel.open(Paths.get("upload", "incoming"), opts, workerPool)
					activeChannel.set(attrs, channel)
				}

				def contentLength = request.contentLengthLong
				// Hash the uploaded content using MD5
				def md5 = md5hash.get(attrs)
				if (!md5) {
					md5 = MessageDigest.getInstance("MD5")
					md5hash.set(attrs, md5)
				}
				def bytes = bytesRead.get(attrs)
				if (!bytes) {
					bytes = new AtomicLong(0)
					bytesRead.set(attrs, bytes)
				}
				// Use a non-blocking input stream
				def inputStream = request.getInputStream(false)

				def handler = completionHandler.get(attrs)
				if (!handler) {
					handler = new CompletionHandler<Integer, byte[]>() {
						def position = 0

						@Override void completed(Integer read, byte[] data) {
							def len = data.length
							position += len
							md5.update(data)
							if (position == contentLength && len == 0) {
								bytesUploaded.addAndGet(position)
								def digest = md5.digest()
								log.info("Upload complete. Hash: " + Base64.encodeBase64String(digest))
							}
						}

						@Override void failed(Throwable throwable, byte[] data) {
							log.error(throwable.message, throwable)
						}
					}
					completionHandler.set(attrs, handler)
				}

//				def inputStreamBuffer = inputStream.getBuffer()
//				def byteBuff = inputStreamBuffer.toByteBuffer()
				inputStream.notifyAvailable(new ReadHandler() {
					@Override void onDataAvailable() {
						readAvailable()
						inputStream.notifyAvailable(this)
					}

					@Override void onError(Throwable t) {
						log.error(t.message, t)
					}

					@Override void onAllDataRead() {
						readAvailable()
						response.resume()
					}

					def readAvailable() {
//						def pos = inputStreamBuffer.position()
						def len = inputStream.readyData()
//						def buff = ByteBuffer.allocateDirect(len)
						byte[] b = new byte[len]
						inputStream.read(b)
						channel.write(ByteBuffer.wrap(b), bytes.get(), b, handler)
						bytes.addAndGet(len)
//						inputStreamBuffer.position(pos)
//						inputStream.skip(len)
					}
				}, BUFFER_SIZE)

				break
		}

	}

	class WriteData {
		byte[] data
		boolean atEnd = false

		@Override String toString() {
			return "data=(${data.length} bytes), atEnd=$atEnd"
		}


	}

}