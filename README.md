# csci421-project

We compiled the project on the cs machines via:

javac -d <wheretocompile> $(find <whereitis/src/java> -name "*.java")

We ran it with:

java -cp <wherecompiled> JottQL <dbLocation> <pageSize> <bufferSize> <indexing>