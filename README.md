# Seating Plan

## The problem

During the Coronavirus lockdown whilst working from home my company set up regular lunch get togethers so people could stay in touch. This involved several meetings on Google Meet with a meeting (room or table) being hosted by one person with other people moving around the tables for different courses.

For us there were typically around 20 people with 4 or 5 tables and 4 courses.

What we found was that however we arranged the tables people often overlapped with other people on several tables or followed each other around when changing courses.

As we are a company of software developers several of us had ideas how to solve this. This is my attempt.

## A model

As I'm a Java developer this is all in Java!.

To model the problem a set of Java classes were created to represent the tables, courses and people. These are the wrapped up into a class to represent the problem or the *Scenario*. This specifies the number of tables, the number of courses, the list of people and who has been allocated as a table host.

The problem can have many solutions, some good some bad and some mediocre. These are modelled as a *Solution* class.

In order to see how good the solution is we need some way of measuring it. There are many criteria that can be used for this but what it all boils down to is "is this solution better than another solution". We just need a simple numeric score.

The problem is to try and make sure people meet as many other people as possible. We can do this by counting how many different people each person meets and then total all these counts. The number of tables a person sits at is also important - we want people to move around the tables and hosts. So we can count those too.

As a starting point I went for counting how many each person met and then combined this with how many tables each person visited, each measure weighted equally.

## Solution #1 - trial and error

One way to attack the problem is to randomly allocate people to tables, measure the solution and then try again repeating and keeping the best solution. Then do this many 1000's of times. In other words make no attempt to come up with a good solution, just guess until one hopefully appears.

## Solution #2 - swap people between tables

Another approach is to come up with a solution, it doesn't have to be a good solution, and then tweak the solution a little to see if it makes it better. We could generate a random solution and then swap people between tables at random, if it makes the score better then keep that solution and repeat from there otherwise drop back the the original and try swapping different people.

I suspect this algorithm may have a tendency to get stuck in local minima - it finds a solution then if there is no better solution to be found by making a simple change but there are better solutions if a few changes are made. So, some scope for improvement.

## Solution #2 - combined

Finally we can take the two solutions and combine, use the first method to give us a good starting point then tweak using the second method. The second algorithm is so much better than the first that this has little benefit.

## Results

So, with 4 courses, 5 tables and 23 people the random try and try again solution typically came up with solutions between 0.65 and 0.85 but often required 100000 runs to get the higher values. The swap and repeat approach averaged 0.90+ with far fewer iterations. On my laptop both only take a few seconds.

## Running the app

The is a simple command line app and uses the picocli lib for parsing arguments

To run with 4 courses, 5 tables, iterate the solution 10000 times using the swap strategy, set the random seed to 1 (so random but repeatable) and using a file to specify the list of people the command would be

`java -jar seatingplan-0.0.1-SNAPSHOT-jar-with-dependencies.jar -s 1 -c 4 -t 5 -i 10000 -st swap -pf people.txt`

Alternatively there is a .bat file that make this a bit easier to run

This requires JDK11 (for no other reason that the code was written and compiled using JDK11).

## Example output

Solution score 0.921196

| Name         | Course 1     | Course 2     | Course 3     | Course 4     | # People     | # Tables     |
| ------------ | ------------ | ------------ | ------------ | ------------ | ------------ | ------------ |
| Alice        | Table 1      | Table 1      | Table 1      | Table 1      | 16           | 1            |
| Bob          | Table 2      | Table 2      | Table 2      | Table 2      | 16           | 1            |
| Charlie      | Table 3      | Table 3      | Table 3      | Table 3      | 16           | 1            |
| Dan          | Table 4      | Table 4      | Table 4      | Table 4      | 12           | 1            |
| Eve          | Table 5      | Table 5      | Table 5      | Table 5      | 12           | 1            |
| Faith        | Table 1      | Table 3      | Table 5      | Table 2      | 13           | 4            |
| Grace        | Table 3      | Table 2      | Table 1      | Table 4      | 14           | 4            |
| Heidi        | Table 1      | Table 5      | Table 2      | Table 4      | 13           | 4            |
| Ivan         | Table 5      | Table 4      | Table 1      | Table 2      | 14           | 4            |
| Judy         | Table 4      | Table 3      | Table 1      | Table 5      | 14           | 4            |
| Kit          | Table 2      | Table 3      | Table 4      | Table 1      | 13           | 4            |
| Laura        | Table 1      | Table 4      | Table 2      | Table 3      | 14           | 4            |
| Mallory      | Table 3      | Table 1      | Table 2      | Table 5      | 12           | 4            |
| Niaj         | Table 5      | Table 2      | Table 4      | Table 3      | 14           | 4            |
| Olivia       | Table 2      | Table 1      | Table 5      | Table 3      | 14           | 4            |
| Peggy        | Table 2      | Table 5      | Table 1      | Table 3      | 14           | 4            |
| Quentin      | Table 3      | Table 1      | Table 2      | Table 5      | 12           | 4            |
| Rupert       | Table 4      | Table 2      | Table 3      | Table 1      | 12           | 4            |
| Sybil        | Table 3      | Table 2      | Table 5      | Table 1      | 13           | 4            |
| Trent        | Table 1      | Table 3      | Table 4      | Table 2      | 12           | 4            |
| Uma          | Table 2      | Table 4      | Table 3      | Table 1      | 13           | 4            |
| Yoko         | Table 5      | Table 1      | Table 3      | Table 4      | 14           | 4            |
| Zac          | Table 4      | Table 5      | Table 3      | Table 2      | 13           | 4            |

