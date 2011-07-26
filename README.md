# Node.js vs. Groovy Shootout

I created this small project to encapsulate a single use case: uploading files (50, 100, 1000MB). There is a Node.js
and a Groovy (using the Grizzly NIO framework) version.

The Groovy version outperforms the Node.js version by a factor of 10 in the first test. This is because the
MD5 algorithm in Node.js is inefficient. If we comment out this call to make it as fast as possible, then the
Groovy version outperforms the Node.js version 40ms.

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

## An your point?

Moral of the story is that JVM-powered applications *can be* as fast as Node.js applications. It's not a
given that simply writing using Node.js as an application platform will automagically give you better
performance. Notice that in this test, there was *absolutely nothing* going on in the Node.js application. Since
Node is single-threaded, any other application level processing in this application will affect these performance
times.

You'll need a 1.0-milestone-4 snapshot version of Gradle because of a problem with JDK 7 giving a weird
error (at least on Mac OS X).

Of course you'll also need Java 7.
