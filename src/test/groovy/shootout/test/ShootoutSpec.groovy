package shootout.test

import org.glassfish.grizzly.http.server.HttpServer
import org.glassfish.grizzly.http.server.NetworkListener
import org.glassfish.grizzly.http.server.ServerConfiguration
import org.glassfish.grizzly.strategies.SameThreadIOStrategy
import shootout.UploadHandler
import spock.lang.Specification

/**
 * @author Jon Brisbin <jon@jbrisbin.com>
 */
class ShootoutSpec extends Specification {

	HttpServer server
	ServerConfiguration config

	def setup() {
		server = new HttpServer()
		def listener = new NetworkListener("shootout", "127.0.0.1", 8080)
		listener.transport.IOStrategy = SameThreadIOStrategy.instance
		listener.transport.readBufferSize = 512000
		listener.transport.tcpNoDelay = true
		server.addListener(listener)

		config = server.serverConfiguration
		config.jmxEnabled = true
		config.addHttpHandler(new UploadHandler(), "/upload")
		server.start()
	}

	def cleanup() {
		while (true) {
			Thread.sleep(5000)
		}
		server.stop()
	}

	def "Test status"() {

		when:
		def t = new URL("http://localhost:8080/upload")?.text

		then:
		null != t

	}

}
