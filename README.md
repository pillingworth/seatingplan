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

## Solution #3 - combined

Finally we can take the two solutions and combine, use the first method to give us a good starting point then tweak using the second method. The second algorithm is so much better than the first that this has little benefit.

## Results

So, with 4 courses, 5 tables and 23 people the random try and try again solution typically came up with solutions between 0.65 and 0.85 but often required 100000 runs to get the higher values. The swap and repeat approach averaged 0.90+ with far fewer iterations. On my laptop both only take a few seconds.

## Running the app

The is a simple command line app and uses the picocli lib for parsing arguments

To run with 4 courses, iterate the solution 10000 times, set the "maximize mixing people weighting" to 0.4 , set the "maximise number of tables sat at weighting" to 0.6, selecting the "swap" strategy, set the random seed to 1 (so random but repeatable) and using a file to specify the list of people the command would be

```
java -jar seatingplan-0.0.1.jar -s 1 -c 4 -i 10000 -dpw 0.4 -dtw 0.6 -st swap -pf people.txt
```

The project is set up to compile with JDK11 but will run on JDK8+.

## Example output

Solution score 0.939130

| Name                 | Course 1 | Course 2 | Course 3 | Course 4 | # People | # Tables |
| -------------------- | -------- | -------- | -------- | -------- | -------- | -------- |
| **(h) Alice       ** | Alice    | Alice    | Alice    | Alice    | 16       | 1        |
| **(h) Bob         ** | Bob      | Bob      | Bob      | Bob      | 13       | 1        |
| **(h) Charlie     ** | Charlie  | Charlie  | Charlie  | Charlie  | 15       | 1        |
| **(h) Dan         ** | Dan      | Dan      | Dan      | Dan      | 14       | 1        |
| **(h) Eve         ** | Eve      | Eve      | Eve      | Eve      | 14       | 1        |
| Faith                | Alice    | Charlie  | Eve      | Dan      | 13       | 4        |
| Grace                | Bob      | Alice    | Eve      | Charlie  | 12       | 4        |
| Heidi                | Dan      | Eve      | Bob      | Alice    | 12       | 4        |
| Ivan                 | Charlie  | Dan      | Bob      | Alice    | 13       | 4        |
| Judy                 | Alice    | Charlie  | Bob      | Dan      | 12       | 4        |
| Kit                  | Eve      | Charlie  | Dan      | Alice    | 15       | 4        |
| Laura                | Bob      | Dan      | Alice    | Eve      | 14       | 4        |
| Mallory              | Charlie  | Eve      | Alice    | Dan      | 14       | 4        |
| Niaj                 | Eve      | Bob      | Alice    | Charlie  | 15       | 4        |
| Olivia               | Charlie  | Alice    | Dan      | Eve      | 14       | 4        |
| Peggy                | Dan      | Bob      | Eve      | Alice    | 14       | 4        |
| Quentin              | Alice    | Dan      | Charlie  | Bob      | 12       | 4        |
| Rupert               | Eve      | Alice    | Charlie  | Bob      | 12       | 4        |
| Sybil                | Charlie  | Bob      | Dan      | Eve      | 13       | 4        |
| Trent                | Alice    | Eve      | Dan      | Charlie  | 15       | 4        |
| Uma                  | Dan      | Charlie  | Alice    | Bob      | 15       | 4        |
| Yoko                 | Dan      | Bob      | Charlie  | Eve      | 13       | 4        |
| Zac                  | Bob      | Alice    | Eve      | Charlie  | 12       | 4        |