# Node.js vs. Groovy Shootout

This is not a "let's pick on [Node.js || Groovy]" type of thing. I'm not interested in fanboyism on either side,
though I would love for some competitiveness to shine through here and illustrate how we can make both apps faster.
That said, this isn't a benchmark of some kind. The Groovy version, for instance, does a whole lot more with
maintaining state during its processing than the Node.js version does simply because of the nature of writing Grizzly
applications. There's really no way to make this "fair" in the sense that both are executing the same number of
instructions. That's not really the point. The point is to find a way to do a particular task that Node.js does
*very well* and translate that to the fastest possible way to do that in Groovy, Java, Scala, or whatever other JVM
language you want to use.

I'm interested in having a discussion about the best way to write non-blocking applications on the JVM that are as
performant as they can be. To do that, I'm trying to isolate what I think is the most obvious raw performance test:
file upload. There is a Node.js upload server and a Groovy (using the Grizzly NIO framework) upload server.

The Groovy version outperforms the Node.js version by a factor of 14 in the first test. This is because the
MD5 algorithm in Node.js is inefficient. If we comment out this call to make it as fast as possible, then the
Groovy version outperforms the Node.js version by 40ms. That's not statistically significant. That's simply pretty
darn fast for both platforms. In my mind, that means Groovy did just as well as Node.js.

This test was uploading a 100MB file 10 times with 1 concurrent user.

#### Groovy (with hash calculation)

    Requests per second:    3.49 [#/sec] (mean)
    Time per request:       286.594 [ms] (mean)

#### Node.js (with hash calculation)

    Requests per second:    0.24 [#/sec] (mean)
    Time per request:       4186.901 [ms] (mean)

#### Node.js (without hash calculation)

    Requests per second:    3.06 [#/sec] (mean)
    Time per request:       326.937 [ms] (mean)


This test was uploading a 1MB file 10 times with 1 concurrent user.

#### Groovy (with hash calculation)

    Requests per second:    155.57 [#/sec] (mean)
    Time per request:       6.428 [ms] (mean)

#### Node.js (without hash calculation)

    Requests per second:    129.06 [#/sec] (mean)
    Time per request:       7.748 [ms] (mean)

## And your point?

Moral of the story is that JVM-powered applications *can be* as fast as Node.js applications. It's not a
given that simply using Node.js as an application platform will automagically give you better performance.
Notice that in this test, there was *absolutely nothing* going on in the Node.js application. Since
Node is single-threaded, any other application level processing in here will affect performance times.

#### To run the tests

You'll need a 1.0-milestone-4 snapshot version of Gradle because of a problem with JDK 7 giving a weird
error (at least on Mac OS X).

Of course you'll also need Java 7.

Simply run `gradle test` from the project directory to run the Grizzly HTTP server. It will appear to "hang", which
is simply the test thread blocking while you hit the server with requests.

To run the Node.js version, cd into `src/main/nodejs` and do `node server.js`. Same thing there. The command shell
will block while you do the testing.

I'm using Apache Bench to do testing, but you can use whatever tool you like. This isn't a benchmark, after all, just
a very informal, completely unscientific test.