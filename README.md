# Palermo

Palermo is a job processing system for the JVM inspired by Resque, backed by
RabbitMQ and written in Clojure.

The library is just a thin layer on top of RabbitMQ with the following
features:

+ Defining jobs
+ Defining job queues
+ Defining workers
+ Serialisation/deserialisation of jobs
+ Queue management functionality

Palermo also includes a web front-end to manage the system  that can
be run as a standalone Jetty application.


## Installation

If you plan to use Palermo as a library, you can grab the latest release from Clojars Maven repository.

If you want to use Palermo to run the web frontend or to start standalone workers from the command line, the easiest way is to clone the project and build an executable standalone jar using leiningen:

```bash
palermo $ lein uberjar
```

## Usage

The following section will review the main functionality offered by Palermo using code snippets in Clojure and Java.

### Connecting

Palermo implements a certain usage pattern (a job processing system) on top of RabbitMQ. To work it needs to connect to a RabbitMQ exchange where all the job queues will be created.
Parameters like the RabbitMQ server, port, username, password, virtual host and exchange name can be passed to the initialisation function.

The following samples show how to establish this connection.

```clojure
(use 'palermo.core)

; Default values: 
; host: localhost 
; port: 5672 
; username: guest 
; password: guest
; vhost /
; exchange palermo
(def palermo-server (palermo {:host rabbit-host}))
```

```java
// overloaded constructor
palermo.PalermoServer palermoServer = new palermo.PalermoServer(host, port, exchange);
```


### Defining jobs

Jobs in Palermo are classes with a default constructor and that implement the `palermo.job.PalermoJob` interface.
The interface defines a single method `process` that will recive the arguments for the job sent to the working queue.

In the following example we will define a job that will just make the worker thread sleep for an interval of time before printing a message on standard output.

In clojure the macro `defpalermojob` can be used to define a Palermo job that will be compiled into the right Java class.

```clojure
(ns palermotests
    (:use palermo.core))

(defpalermojob SleepyJob 
  (process [job timeout]
    (println "SLEEPING...")
    (Thread/sleep timeout)
    (println "BACK!")))
```

The following code is the equivalent Java implementation. It is important to define a default constructor without arguments for Palermo to instantiate the job class correctly.

```java
package palermotests;

import palermo.job.PalermoJob;

public class SleepyJob implements PalermoJob {

    @Override
    public void process(Object arguments) {
        int timeout = (Integer) arguments;
        System.out.println("SLEEPING...");
        try {
            Thread.sleep(timeout);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        System.out.println("BACK!");
    }

}
```

### Enqueuing jobs

Once we have opened a Palermo connection to RabbitMQ, we can enqueue jobs into the system that will be picked and processed by workers.

In order to enqueue a job we need to define a tuple with three components:

- The type of the job
- The queue where the job will be inserted
- The arguments for the job

The following code show how a jobe can be enqueued in Clojure:

```clojure
(let [queue-name "tests"
      timeout-seconds-argument 5]
  (enqueue palermo-server queue-name 
                          palermotests.SleepyJob 
                          timeout-seconds-argument))
```

In the same way, using the Java interface:

```java
String queueName = "slow";
Integer timeoutArgument = 5;

palermoServer.enqueue(queueName, 
                      palermotests.WaitJob.class, 
                      timeoutArgument);
```

When a job is enqueue, the arguments passed to the `enqueue` function will be serialised and sent to the Palermo job queue.
When a worker picks the job from the queue, Palermo will deserialise the argument, instantiate the job class and invoke the `process` method of the job passing the deserialised object as the argument in the invokation.

JBoss serialisation is the standard mechanism used by Palermo to serialise and deserialise job arguments. In order for the enqueuing/processing mechanism of Palermo work, certain constraints must be taken into account:

- The job class must be available in the class path of the publisher and in the class path of the worker picking the job.
- Job classes must have a default constructor without argument.
- Argument objects need to be supported by JBoss serialisation,  basic objects, containers or POJOs with a default constructor are supported.


### Starting and stopping workers

Workers are threads managed by Palermo that will pick jobs from Palermo's job queues and will process the jobs with the provided arguments.
Workers can connect to multiple queues and if multiple workers are connected to the same queue, jobs will be distributed among them in a round-robbin fashion.

To start a worker programmatically, the following Clojure and Java code can be used:

```clojure
(start-worker palermo-server ["high" "low" "tests"])
```

```java
String[] queues = {"high", "low", "tests"};
palermoServer.startWorker(queues);
```

Workers can also be stopped provided the identifier of th worker.
The identifier can be retrieved using the `workers` function or method.

```clojure
; stopping all the workers
(doseq [worker-id (workers palermo-server)]
  (stop-worker palermo-server worker-id))
```

```java
// stopping all the workers
for(String workerId : palermoServer.workers())
  palermoServer.stopWorker(workerId);
```

### Inspecting queues and workers

Runtime information about the status of the job queues or the connected workers can be accomplished with a small set of Clojure functions and Java methods provided by the Palermo connection.

```clojure
; summary information about workers, pending jobs
; and jobs being processed per queue
(queues-info palermo-server)

; information about the workers
(workers-info palermo-server)

; enumeration of the worker identifiers in the system
(workers palermo-server)

; enumeration of the jobs pending in a particular queue
(jobs-in-queue palermo-server "tests")
```

```java
// summary information about workers, pending jobs
// and jobs being processed per queue
HashMap info = palermoServer.getQueuesInfo();

// information about the workers
HashMap workers = palermoServer.getWorkersInfo();

// enumeration of th worker identifiers in the system
String[] workerIds = palermoServer.workers();

// enumeration of the jobs pending in a particular queue
ArrayList jobs = palermoServer.getQueueJobs("tests");
```
Jobs in a queue can also be removed in a single invocation using the `purge` function.

```clojure
(purge-queue palermo-server "tests")
```

```java
palermoServer.purgeQueue("tests");
```


### Dealing with errors

If a worker throws an exception during the processing of a job, the job will be re-enqueued into a special queue named *failures*.

The jobs in this queue can be inspected using the queue inspection functions and individual failed jobs or all the failed jobs can be re-enqueued programatically.

```clojure
;; retrying all the failed jobs 1 by 1
(doseq [failed-job (jobs-in-queue palermo-server "failed")]
  (let [failed-job-id (-> failed-job :metadata :message-id)]
    (retry-failed-job palermo-server failed-job-id)))
```

```java
// retrying all the failed jobs 1 by 1
ArrayList<HashMap> jobs = palermoServer.getQueueJobs("failed");
for(HashMap job : jobs){
  HashMap metadata = (HashMap) job.get("metadata");
  String messageId = (String) metadata.get("message-id");

  palermoServer.retryJob(messageId);
}
```

All the failed jobs can also be re-enqued in a single invocation using the `retry-all` function.

```clojure
(retry-all palermo-server)
```

```java
palermoServer.retryAllFailedJobs();
```

## Command line interface

Palermo includes a command line client that can be used to start a worker that will automatically start processing jobs from the Palermo RabbitMQ connection configured according the parameters passed in the command line.

The list of possible command line options are shown in the following snippet.

```bash
$ java -jar palermo-standalone.jar
usage: worker
 -exchange <EXCHANGE>   RabitMQ exchange for Palermo, defaults to palermo
 -host <HOST>           Host of the RabbitMQ server, defaults to localhost
 -password <PASSWORD>   Password for the RabbitMQ server, defaults to
                        guest
 -port <PORT>           Port of the RabbitMQ server, defaults to 5672
 -queues <QUEUES>       Comma separated list of queues this worker will
                        connect to, defaults to jobs
 -username <USERNAME>   Username for the RabbitMQ server, defaults to
                        guest
 -vhost <VHOST>         Virtual Host for the RabbitMQ server, defaults to
```

In order for the worker to be able to process the jobs, the jobs class files must be available in the class path of the worker.

## Web interface

Palermo includes a web interface that can be used to manage the queues and perform common operations like retrying failed jobs.

![palermo web](https://raw.github.com/antoniogarrote/palermo/Master/docs/images/palermo.png)

To start the web frontend a command line utility is provided. These are all the option available to the command line launcher, accessible through the `palermo.cli` class or through the executable jar that is built when invoking `lein uberjar`.

```bash
usage: web
 -exchange <EXCHANGE>   RabitMQ exchange for Palermo, defaults to palermo
 -host <HOST>           Host of the RabbitMQ server, defaults to localhost
 -password <PASSWORD>   Password for the RabbitMQ server, defaults to
                        guest
 -port <PORT>           Port of the RabbitMQ server, defaults to 5672
 -username <USERNAME>   Username for the RabbitMQ server, defaults to
                        guest
 -vhost <VHOST>         Virtual Host for the RabbitMQ server, defaults to
 -webport <WEBPORT>     Port where the Palermo web interface will be
                        waiting for connections, defaults to 3000
```
## License

Copyright © 2014 Antonio Garrote

Distributed under the Eclipse Public License either version 1.0 or (at
your option) any later version.


Published under the Eclipse Public License (See the LICENSE file).
2014 (c) Antonio Garrote <antoniogarrote@gmail.com>