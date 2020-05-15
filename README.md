# Mongo Command-line Tool

With this tool you can match a stream of JSON objects against a MongoDB [query expression](https://www.javadoc.io/static/net.pincette/pincette-mongo/1.3/net/pincette/mongo/Match.html) and validate JSON objects against a MongoDB-based [validation specification](https://www.javadoc.io/static/net.pincette/pincette-mongo/1.3/net/pincette/mongo/Validator.html).

The tool reads JSON objects from ```stdin``` and writes matching or validated objects to ```stdout```. Objects that fail validation will produce a JSON object with the field ```errors```, which is an array of objects. These will have a ```location``` field, which is a JSON pointer and optionally a ```code``` field if the corresponding condition in the validation specification has the ```$code``` field.


You can build it with ```mvn clean package```.

Run it as follows:

```
> java -jar target/pincette-mongo-cli-<version>-jar-with-dependencies.jar
    match
        (-q | --query-file) query_file |
    validate
        (-s | --specification-file) specification_file
    [-t | --trace]        
```

|Option|Description|
|---|---|
|-q \| --query-file|The file that contains a MongoDB query expression.|
|-s \| --specification-file|The file that contains a validation specification.|
|-t \| --trace|This turns on tracing of expression evaluation for both the ```match``` and ```validate``` commands.|
