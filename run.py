#!/usr/bin/python

import sys, subprocess

entryPoints = [

    ("Server"            , "com.golimojo.Server"),
    ("ExtensionBuilder"  , "com.golimojo.ExtensionBuilder"),
    ("PageDumpReader"    , "com.golimojo.PageDumpReader"),
    ("Service"           , "com.golimojo.Service"),

]

lowerCaseEntryPointDict = dict([ (key.lower(), value) for key, value in entryPoints ])

def main():

    # Get the script name.  Note that "rfind(<<< character-not-in-string >>>) + 1" ==> 0.
    scriptName = sys.argv[0]
    scriptName = scriptName[scriptName.rfind("/") + 1:]

    if len(sys.argv) != 2:
        print >> sys.stderr, "Error: Expected a single argument!"
        print >> sys.stderr, "Use:"
        print >> sys.stderr, "    %s help" % scriptName
        print >> sys.stderr, "for help."
        print >> sys.stderr, "Exit 1."
        sys.exit(1)

    elif sys.argv[1] in ["help", "--help", "-help", "-h"]:
        help(scriptName)
        sys.exit(0)

    else:
        entryPointAlias = sys.argv[1]
        run(scriptName, entryPointAlias)
        pass

def help(scriptName):
    print
    print "%s:" % scriptName
    print
    print "A handy tool for running a Java program under Maven using 'mvn java:exec'."
    print
    print createHelpExample(scriptName, "help", "displays this help message")
    print
    for entryPointTuple in entryPoints:
        alias, className = entryPointTuple
        message = "runs the %s entry point" % className
        print createHelpExample(scriptName, alias, message)
    print
    print "Note that entry point aliases are NOT case sensitive.  So the following are equivalent:"
    print
    print createHelpExample(scriptName, entryPoints[0][0])
    print createHelpExample(scriptName, entryPoints[0][0].lower())
    print 

def run(scriptName, entryPointAlias):

    # Look up the entry point.
    try:
        entryPointClassName = lowerCaseEntryPointDict[entryPointAlias.lower()]
    except (KeyError):
        print >> sys.stderr, "Unknown entry point alias: '%s'" % entryPointAlias
        print >> sys.stderr, "Use '%s help' to list available aliases." % scriptName
        print >> sys.stderr, "Exit 1."
        sys.exit(1)

    # Chain to Maven.
    try:
        returnCode = subprocess.call(["mvn", "compile", "exec:java", "-Dexec.mainClass=" + entryPointClassName])
        sys.exit(returnCode)
    except (KeyboardInterrupt):
        print >> sys.stderr, "Process terminated by keyboard interrupt."
        print >> sys.stderr, "Exit 1."
        sys.exit(1)

def createHelpExample(scriptName, entryPointAlias, message=None):
    line = "    %s %s" % (scriptName, entryPointAlias)
    if message is not None:
        line = "%-40s - %s" % (line, message)
    return line


# Invoke the main function.
if __name__ == '__main__':
    main()

