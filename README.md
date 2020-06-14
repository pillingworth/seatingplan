# seatingplan

## The probem

During the Coronavirus lockdown whilst working from home my company set up regular lunch get togethers so people could stay in touch. This involved several meetings on Google Meet with a meeting (room or table) being hosted by on eperson with other people moving around the tables for different courses.

For us there were typically around 20 people with 4 or 5 tables and 4 courses.

WHat we found was that however we arranged the tables we found people often overlapped with other people on several tables or followed each other around.

As we are a company of software developers several of us had ideas how to solve this. This is my attempt.

## A model

As I'm a Java developer all the solutions are in Java!.

To model the problem a set of Java classes were created to represent the tables, courses and people. These are the wrapped up into a class to represent the problem or the scenario. This specifies the number of tables, the number of courses, the list of people and who has been allocated as a table host.

The problem can have many solutions, some good some bad and some mediocre. There are modelled as a solution class.

In order to see how good the solution is we need some way of measuring it. There are many criteria that can be used for this but it all boils down to is this solution better than another solution - so we need a simple numeric score.

The problem is get make sure people meet as many people as possible. So we can simply count how many different people each person met and the total these. This is a little simplisitic as we might end up with some people not mixin with many people so we could include variance or have validity crietria such as each perosn has to meet a certain number of other people for the solution to be valid.

As a starting point I went for totally how many each person met combined with how many tables each person visited, each weighted equally.

## Solution #1

One way to attach the problem is to randomly allocate people to tables, measure the solution and then try again repeated keeping the best solution. Do this many 1000's of times. In other words make no attempt to come up with a good solution, just repeat until one hopefully appears.

## Solution #2

Another approach is to come up with a solution and then if we were doing this manually tweak the solution a little to see if it makes it better. So, generate a random solutions then swap people around at random, if it makes the score better then keep that solution and repeat from there otherwise drop back the the original and try swapping different people.

The tweak here is to simply swap two people over.

I suspect this algorithm has a tendency to get stuck in local minima - it finds a solution then there is no better solution to be found by making a simple change but there are better solutions if a few changes are made. So, some scope for improvement.

## Combined

Finally we can take the two solutions and combine, use the first method to give us a good starting point then tweak using the second method. The second algorithm is so much better than the first that this has little benefit.

## Results

So, with 4 courses, 5 tables and 23 people the random try and try again solution typically came up with solutions between 0.65 and 0.85 but often required 100000 runs to get the higher values. The tweak and repeat approach averaged 0.92 with far fewer iterations.

## Running the app

The is a simple command line app and uses the picocli lib for parsing arguments

To run with 4 courses, 5 tables, iterate the solution 10000 times using the swap strategy, set the random seed to 1 (so reandom but repeatable) and using a file to specify the list of people the command would be

`java -jar seatingplan-0.0.1-SNAPSHOT.jar -s 1 -c 4 -t 5 -i 10000 -st swap -pf people.txt`

This require JDK11 (for no other reason that the code was written and compiled using JDK11).
