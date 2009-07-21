# DSV

Create and parse CSV in Clojure.

## Installation

1. Download the library source code, using git: 
   `git clone git://github.com/deriksson/dsv.git`.
2. Compile the source code and create a jar file by running ant from the
   folder that was created in the first step: `cd dsv;ant`.
3. The build command will create a jar in the root folder of the new
   directory. Include this jar in your classpath.
4. You will also need clojure contrib. A version of this library is
   included in the lib directory.

## Creating CSV

The function for creating CSV is in the namespace `se.abc.dsv.write`.
Make it available to your program like this:

    (use 'se.abc.dsv.write)

At its simplest, you create CSV by passing a dataset in the form of a
vector of vectors of strings to the function `write-csv`:

    (write-csv [["apple" "fruit"]["monkey" "animal"]])

You can use duck streams to create a CSV file:

    (use 'clojure.contrib.duck-streams)
    (spit "/tmp/stats.csv" (write-csv [["apple" "fruit"]["monkey" "animal"]]))

Customise the CSV by specifying field delimiter and quote character.
In this example semicolons are used instead of commas as
field delimiters:

    (write-csv [["apple" "fruit"]["monkey" "animal"]] \;)

In the following example single quotes are used instead of the default
double quotes for preventing delimiter collisions:

    (write-csv [["apple, red" "fruit"]["apple, green" "fruit"]] \, \')

## Parsing CSV

The namespace for the CSV parser is `se.abc.dsv.read`:

    (use 'se.abc.dsv.read)

The following simple example uses the default settings, and will create
a vector containing two vectors, each containing two strings.

    (read-csv "apple,fruit\nmonkey,animal")

Use duck-streams to read a CSV file like this:

    (use 'clojure.contrib.duck-streams)
    (read-csv (slurp* "/tmp/stats.csv"))

The parser will raise an error if the CSV is not well-formed. Use 
error-kit from clojure contrib to handle errors:

    (use 'clojure.contrib.error-kit)  
    (with-handler  
     (read-csv (slurp* "/tmp/stats.csv"))  
     (handle malformed-csv [msg] (println "Malformed CSV.")))

The CSV function can be customised to handle other field delimiters
and quote characters than the defaults (',' and '"'):

    (read-csv "apple;fruit\nmonkey;animal" \;)

    (read-csv "'apple, red',fruit" \, \')
