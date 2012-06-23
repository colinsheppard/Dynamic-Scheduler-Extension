# NetLogo Dynamic Scheduler Extension

This package contains the NetLogo dynamic scheduler extension, which provides support for a new data type (schedule objects) and associated operations (adding, removing, and dispatching events on the schedule) in NetLogo. 

## What is it?

The dynamic scheduler extension enables a different approach toward scheduling actions in Netlogo.  Traditionally, a Netlogo modeler puts a series of actions or procedure calls into the "go" procedure which is executed at the beginning of every tick.  Sometimes it is more natural or more efficient to instead say "have agent X execute procudure Y at time Z".  This is what dynamic scheduling enables. 

## When is dynamic scheduling useful?

Dynamic scheduling is most useful for models where agents spend a lot of time not doing anything *even though* the modeler knows when they need to act next. Sometimes in a netlogo model, you end up testing a certain condition or set of conditions for every agent every tick (usually in the form of an “ask”), just waiting for the time to be ripe.... this can get cumbersome and expensive.  In some models, you might know in advance exactly when a particular agent needs to act. Dynamic scheduling cuts out all of those superfluous tests.  The action is performed only when needed, with no condition testing and very little overhead.

For example, if an agent is a state machine and spends most of the time in the state “at rest” and has a predictable schedule that knows that the agent should transition to the state “awake” at tick 105, then using a dynamic scheduler allows you to avoid putting something like "if ticks == 105 \[ do-something \]", which has to be evaluated every tick!

## Installing

First, [download the latest version of the extension](https://github.com/colinsheppard/Dynamic-Scheduler-Extension/tags). Note, the latest version of this extension was compiled against Netlogo 5.0.1, if you are using a different version of Netlogo you might consider building your own jar file (see section "Building" below).

Unzip the archive, and you will get a directory containing the extension jar file.  Rename this directory "dynamic-scheduler" and move it to the "extensions" directory inside your Netlogo application folder.

Now add "extensions\[dynamic-scheduler\]" to the top of any Netlogo model where you want to use the extension.  

## Examples

See the example models in the extension subfolder "example" for a demonstration of usage.

## Behavior

dynamic-scheduler has the following behavior:

* If multiple events are scheduled for the same time, they are dispatch in the order in which they are added to the schedule.

* When an agentset is scheduled to perform an event, the individual agents execute the procedure in the same order as *ask*, i.e. the default iteration order used by Netlogo.  To shuffle the order, the agentset must be shuffled during the add (e.g. "(turtle-set (sort-by [true] turtles))").

* If an agent is scheduled to perform a task in the future but dies before the event is dispatched, the event will be silently skipped.

## Primitives

**dynamic-scheduler:create**

*dynamic-scheduler:create*

Reports a dynamic schedule, a custom data type included with this extension, which is used to store events and dispatch them.  All other primitives associated with this extension take a schedule data type as the first argument, so it's usually necessary to store the schedule as a global variable.

    set schedule dynamic-scheduler:create 

---------------------------------------

**dynamic-scheduler:add** 

*dynamic-scheduler:add schedule agent task number*
*dynamic-scheduler:add schedule agentset task number*

Add an event to a dynamic schedule.  The order events are added is not important, events will be dispatched in order of the time passed as the last argument. An *agent* or an *agentset* can be passed as the second argument along with a *task* as the third, which is executed by the agent(s) at *number*, which is a time greater than or equal to the present moment (*>= ticks*).

    dynamic-scheduler:add schedule turtles task go-forward 1.0

---------------------------------------

**dynamic-scheduler:repeat** 

*dynamic-scheduler:repeat schedule agent task time-number repeat-interval-number*
*dynamic-scheduler:repeat schedule agentset task time-number repeat-interval-number*

Add a repeating event to a dynamic schedule.  This behaves almost identical to *dynamic-scheduler:add* except after the event is dispatched it is immediately rescheduled *repeat-interval-number* ticks into the future using the same *agent*/*agentset* and *task*. 

    dynamic-scheduler:repeat schedule turtles task go-forward 2.5 1.0

---------------------------------------

**dynamic-scheduler:go** 

*dynamic-scheduler:go schedule*

Dispatch all of the events in *schedule*.  It's important to note that this will continue until the schedule is empty.  If repeating events are in the schedule or if procedures in the schedule end up scheduling new events, it's possible for this to become an infinite loop.  See the example model "DynamicSchedulerExtension.nlogo" for an example of how to stop the schedule.

    dynamic-scheduler:go schedule

---------------------------------------

**dynamic-scheduler:go-until** 

*dynamic-scheduler:go-until schedule halt-time*

Dispatch all of the events in *schedule* up until *halt-time*.  If the temporal extent of your model is known in advance, this variant on *dynamic-scheduler:go* is the recommended way to dispatch your model.

    dynamic-scheduler:go-until schedule 100.0

## Building

Use the NETLOGO environment variable to tell the Makefile which NetLogoLite.jar to compile against.  For example:

    NETLOGO=/Applications/NetLogo\\\ 5.0 make

If compilation succeeds, `dynamic-scheduler.jar` will be created.

## Author

Colin Sheppard

## Feedback? Bugs? Feature Requests?

Please visit the [github issue tracker](https://github.com/colinsheppard/Dynamic-Scheduler-Extension/issues?state=open) to submit comments, bug reports, or requests for features.  I'm also more than willing to accept pull requests.

## Credits

I relied heavily on the Netlogo matrix extension for guidance and as a template in the development of this extension.  Allison Campbell helped develop the benchmark model to compare dynamic scheduling to the Netlogo status quo of static scheduling.

## Terms of Use

[![CC0](http://i.creativecommons.org/p/zero/1.0/88x31.png)](http://creativecommons.org/publicdomain/zero/1.0/)

The NetLogo dynamic scheduler extension is in the public domain.  To the extent possible under law, Colin Sheppard has waived all copyright and related or neighboring rights.
