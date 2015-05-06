## Introduction

Golimojo is a tool for automatically adding Wikipedia links to existing web pages.  If you have Git, Java, Maven, and Python (the last is optional) then it should be easy to build and run the full system on your development machine.

There is a demo system running at <http://www.golimojo.com>.

## Building and running the system

The project can be built using `mvn compile` and packaged using `mvn package`.  Maven-based deployment is not supported, however.

The project has several main entry points which can be run directly using `mvn exec:java` or more conveniently using the `run.py` Python script.  The most useful command is `python run.py server` which runs the test server.

You can use `python run.py help` to see a list of all entry points.  If you are using **Bash** or a similar shell, you may find this alias convenient: `alias run='python run.py'` -- this way you can simply say `run server`.

## History

The Git repository for Golimojo was created in 2015 but many of the source revisions date from the original closed-source project back in 2005 followed by a few more revisions from 2010 when the the first open-source version of the project was pushed to Google Code.

Each of these early revisions are a combination of changes from 2005 or 2010 with changes from 2015.  Notably, the Maven configuration and the Python "run" script were written in 2015.  This README and the BSD license are also new, though the 2010 revisions were also BSD licensed with slightly different wording.  The core Java, JavaScript, HTML, and CSS files are largely unchanged from their original versions except for a few minor changes to ensure that each revision will compile and run with the 2015 build tooling.

