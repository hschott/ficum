[![Build Status](https://travis-ci.org/hschott/ficum.svg)](https://travis-ci.org/hschott/ficum) [![Maven Central](https://img.shields.io/maven-central/v/com.tsystems.ficum/ficum.svg)](http://search.maven.org/#search%7Cga%7C1%7Ccom.tsystems.ficum) [![Codacy Badge](https://api.codacy.com/project/badge/grade/24b8272573164f8f97eb6e810f8fa6fb)](https://www.codacy.com/app/hschott/ficum)
# FICUM - RESTful Dynamic Filters for JPA, MongoDB and Hazelcast

Are you tired of writing finder methods for every single use case? Do you have to compile, test and deploy your complete service for just a new finder method?


## FICUM in a Nutshell

FICUM is a simple query language that orientates at [FIQL](https://tools.ietf.org/html/draft-nottingham-atompub-fiql-00), tied together with a Parser, a Builder and Visitors for JPA, MongoDB ad Hazelcast.

It is inspired by [Apache CXF JAX-RS Search](http://cxf.apache.org/docs/jax-rs-search.html), a blog entry by [Chris Koele](http://koelec.blogspot.de/2012/06/filter-expressions-in-rest-urls.html) and [rsql-parser](https://github.com/jirutka/rsql-parser).

### tl;dc

```
                                     Builder API
                                     +----------------+
                                                      |
                                                      |
                  +----------+                  +-----v-----+                 +-----------+  Predicate
Query Literal     |          |   Infix Stack    |           |    Node Tree    |           |  or Query Literal
+---------------- >  PARSER  +------------------>  BUILDER  +----------------->  VISITOR  +------------>
                  |          |                  |           |                 |           |
                  +----------+                  +-----------+                 +-----------+
```

**with JPA**

```java
// define selector names allowed to be used in query string
String[] allowedSelectorNames = { "owner.city" , "type"};

// define the query
String input = "owner.city=='Madison',type=='dog'";
// and parse the query into a node tree
Node root = ParseHelper.parse(input, allowedSelectorNames);

// run the JPA visitor on the node tree
JPATypedQueryVisitor<Pet> visitor = new JPATypedQueryVisitor<Pet>(Pet.class, entityManager);
TypedQuery<Pet> query = visitor.start(root);

// and finally get a list of queried entities
List<Pet> results = query.getResultList();
```
Add dependencies for FICUM and JPA

```xml
<dependency>
    <groupId>com.tsystems.ficum</groupId>
    <artifactId>ficum-visitor</artifactId>
    <version>0.4.0</version>
</dependency>
<dependency>
    <groupId>org.hibernate.javax.persistence</groupId>
    <artifactId>hibernate-jpa-2.1-api</artifactId>
    <version>1.0.0.Final</version>
</dependency>
```


**with MongoDB**

```java
// define selector names allowed to be used in query string
String[] allowedSelectorNames = { "address.location", "grades.score" };

// define the query
String input = "address.location=nr=[-73.856077,40.848447,250.0],grades.score=lt=10";
// and parse the query into a node tree
Node root = ParseHelper.parse(input, allowedSelectorNames);

// run the MongoDB visitor on the node tree
MongoDBFilterVisitor visitor = new MongoDBFilterVisitor();
Bson filter = visitor.start(root);

// and finally get a iterable of filtered documents
FindIterable<Document> documents = getMongoDB().getCollection("restaurants").find(filter);
```
Add dependencies for FICUM and MongoDB

```xml
<dependency>
    <groupId>com.tsystems.ficum</groupId>
    <artifactId>ficum-visitor</artifactId>
    <version>0.4.0</version>
</dependency>
<dependency>
    <groupId>org.mongodb</groupId>
    <artifactId>mongodb-driver</artifactId>
    <version>3.2.0</version>
</dependency>
```


**with Hazelcast**

```java
// define selector names allowed to be used in query string
String[] allowedSelectorNames = { "owner.city" , "type"};

// define the query
String input = "owner.city=='Madison',type=='dog'";
// and parse the query into a node tree
Node root = ParseHelper.parse(input, allowedSelectorNames);

// run the JPA visitor on the node tree
HazelcastPredicateVisitor visitor = new HazelcastPredicateVisitor();
Predicate<?, ?> query = visitor.start(root);

// and finally get a list of queried entities
Collection<?> results = getHazelcastInstance().getMap("pets").values(query);

```
Add dependencies for FICUM and Hazelcast

```xml
<dependency>
    <groupId>com.tsystems.ficum</groupId>
    <artifactId>ficum-visitor</artifactId>
    <version>0.4.0</version>
</dependency>
<dependency>
    <groupId>com.hazelcast</groupId>
    <artifactId>hazelcast-client</artifactId>
    <version>3.6.2</version>
</dependency>
```


**as RESTful request parameter**

The query could be passed in via uriencoded query parameter `/pets?q=owner.city%3D%3D'Madison'%2Ctype%3D%3D'dog'`.

**with Builder**

It is also possible to build the node tree with an API and from the node tree a query literal.
The Builder works in infix notation just as you would write the query as string.

```java
Node root = Builder.start().constraint("owner.city", Comparison.EQUALS, "Madison").and()
        .constraint("type", Comparison.EQUALS, "dog").build();
String query = new QueryPrinterVisitor().start(root);
```

The Builder.constraint() method excepts as argument any `java.lang.Comparable`.


## FICUM Query Language

A FICUM query's input is a string of Unicode characters in the form of an expression.

An expression is composed of one or more constraints, related to each other with Boolean operators. Expressions yield Boolean values: True or False.

```
expression  = [ "(" ]
              ( constraint / expression )
              [ operator ( constraint / expression ) ]
              [ ")" ]
operator    = "," / ";" / "." / ":"
```

* `,` is the Boolean **AND** operator; it yields True if both operands evaluate to True, otherwise False.
* `;` is the Boolean **OR** operator; it yields True if either operand evaluates to True, otherwise False.
* `.` is the Boolean **NAND** operator; it yields False if both operands evaluate to True, otherwise True.
* `:` is the Boolean **NOR** operator; it yields False if either operand evaluates to True, otherwise True.

By default, the AND operator takes precedence (i.e., it is evaluated before any OR operators are). However, a parenthesised expression can be used to change precedence, yielding whatever the contained expression yields.

A constraint is composed of a selector, comparison and argument triple, which refines the constraint. When processed, a constraint yields a Boolean value.
An array of arguments must be enclosed in square brackets and each argument separated by a comma.

```
constraint     =  selector comparison ( argument / args-array )
args-array     =  "[" argument *( "," argument ) "]"
```

A selector identifies the field of an entity that a constraint applies to. Since entities can be nested, an selector can also be defined as nested fields with a dot as separator.

```
selector       =  1*selector-char
                  [ 1*( "." 1*selector-char ) ]
selector-char  =  ALPHA / DIGIT / "_"
```

A comparsion yields to True if the argument can be processed against the selector defined entity field value in the following manner:

comparsion | operator       | JPA visitor support | MongoDB visitor support | argument type
------     | ------         | ------              | ------                  | ------
==         | EQUALS         | X                   | X                       | any single argument 
!=         | NOT EQUALS     | X                   | X                       | any single argument
=ge=       | GREATER EQUALS | X                   | X                       | any single argument
=le=       | LESS EQUALS    | X                   | X                       | any single argument
=gt=       | GREATER THAN   | X                   | X                       | any single argument
=lt=       | LESS THEN      | X                   | X                       | any single argument
=nr=       | NEAR           |                     | X                       | argument array of 3 or 4 Double values
=wi=       | WITHIN         |                     | X                       | argument array of 3, 4 or more than 5 Double values
=ix=       | INTERSECTS     |                     | X                       | argument array of 2, 4 or more than 5 Double values



```
comparison     =  "==" / "!=" / "=ge=" / "=le=" / "=gt=" / "=lt=" / "=nr=" / "=wi=" / "=ix="
```

An argument can be one of 5 main types.
**Text, Datetime, Number, Boolean and Null.**

```
argument       =  text-arg / date-arg / number-arg / boolean-arg / null-arg
```

**Examples:**
```
firstname=='Jack'
birthdate==2015-12-24
lastupdate=le=2013-01-04T09:15:00.000+01:00
secure==true
email!=null
points=gt=120;points=le=120,lastplayed=lt=2015-06-05
```


## FICUM Types

The argument's type is negotiated from it's content by a few rules. 

### Date

A date or timestamp is parsed from ISO 8601 string representation and results in a `java.util.Calendar` object.

A simple date without time part will be parsed from the format `yyyy-MM-dd`.

A timestamp will be parsed from the format `yyyy-MM-dd'T'HH:mm:ss.SSSZZ`. The timezone offset value can be either `Z` for UTC or a time value in negative or positive hours, minutes and optional seconds.

**Examples:**

```
2015-12-24 evaluates to 24. December 2015
-645-04-13 evaluates to 13. April 645 BC

2013-01-04T09:15:00.000+01:00 evaluates to 04. January 2013 09:15 AM CET
1492-08-03T15:30:00.000Z evaluates to 03. August 1492 15:30 PM UTC
```

### Number

A number is parsed from a string literal containing digits, sign, decimal dot, qualifier and exponent and results in either `java.lang.Integer`, `java.lang.Long`, `java.lang.Float` or `java.lang.Double`.

**Examples:**

literal    | type    | value
------     | ------  | ------
23         | Integer | 23
856l       | Long    | 856
73L        | Long    | 73
34.01      | Double  | 34.01
5.5d       | Double  | 5.5
67.0D      | Double  | 67.0
34.78e-1d  | Double  | 3.478
912.24f    | Float   | 912.24
2.345F     | Float   | 2.345
210.12E+1f | Float   | 2101.2

### Boolean

A boolean is parsed from a string literal equal to "true" or "false" or "yes" or "no" and results in a `java.lang.Boolean` object.

### Null

A null reference is parsed from a string literal equal to "null".

### Text

A text is parsed from single quoted pct or hex encoded string literals or any other string literal and results in a `java.lang.String` object. For results of length 1 a `java.lang.Character` is returned.

Pct encoded strings must start with `%` followed by two hex digits. Hex encoded strings must start with one of `#`, `0x` or `0X` followed by two or more (even number) hex digits. Both can evaluate to a multibyte char.

**Examples:**

literal         | value
------          | ------
'%25'           | %
'%D4%A2'        | Ԣ
'#d4b1'         | Ա
'0XD58C'        | Ռ
'Hello%20world' | Hello world



## FICUM JPA TypedQuery Visitor

The JPA visitor is capable of traversing a Node tree and converting it to a `javax.persistence.TypedQuery<T>`. The selector names must correspond to the entity `<T>` field names or will be resolved against the visitors `selectorToFieldMapping` Map.

### Text with Wildcards

Text arguments can contain wildcards:
* `?` is a placeholder for one character
* `*` is a placeholder for zero or more characters

When a Test contains a wildcard the comparsion is changed from `EQUALS` to `LIKE` and from `NOT EQUALS` to `NOT LIKE`.

### Enum as Text

When the selector name matches an entity field of type `java.lang.Enum` and the argument is a Text wich results into a `java.lang.String`, then an attempt is made to get the Enum instance and compare it against the entity field value.

### Collection size check

When the selector name matches a `java.util.Collection` field and the argument results into an `java.lang.Integer`, then the collection's size is compared against the argument.



## FICUM MongoDB Filter Visitor

The MongoDB visitor is capable of traversing a Node tree and converting it to a `org.bson.conversions.Bson` filter document. The selector names must correspond to the field names of a MongoDB document or will be resolved against the visitors `selectorToFieldMapping` Map.

### Text with Wildcards

Text arguments can contain wildcards:
* `?` is a placeholder for one character
* `*` is a placeholder for zero or more characters

When a Test contains a wildcard the comparsion is done as regular expression.

### Geospatial comparisons

With the MongoDB Visitor selected [geospatial queries](https://docs.mongodb.org/manual/reference/operator/query-geospatial/) can be executed. The type of MongoDB filter predicate depends on the comparison and the number of arguments used.
Anyway, all arguments in the arguments array must be of type Double.
The order of coordinate values is x, y respectively longitude, latitude.
Polygons are closed automatically by duplicating the first Position to the last Position.
Please also read [Calculate Distance Using Spherical Geometry](https://docs.mongodb.org/manual/tutorial/calculate-distances-using-spherical-geometry-with-2d-geospatial-indexes/).


comparison | number of arguments | MongoDB predicate                         | notation                                          | shape description
----       | ----                | ----                                      | ----                                              | ----
NEAR       | 3                   | $nearSphere                               | address.location=nr=[x,y,maxDistance]             | coordinates of a point and distance in meters
NEAR       | 4                   | $nearSphere                               | address.location=nr=[x,y,maxDistance,minDistance] | coordinates of a point and distances in meters
WITHIN     | 3                   | $geoWithin $centerSphere                  | address.location=wi=[x,y,radius]                  | coordinates of a point and distance in radians 
WITHIN     | 4                   | $geoWithin $box                           | address.location=wi=[x1,y1,x2,y2]                 | bottom left and upper right corners of a rectangle
WITHIN     | >5                  | $geoWithin $polygon                       | address.location=wi=[x1,y1 , ...]                 | list of coordinates of a polygon
INTERSECTS | 2                   | $geoIntersects $geometry type: Point      | address.location=ix=[x,y]                         | coordinates of a point
INTERSECTS | 4                   | $geoIntersects $geometry type: LineString | address.location=ix=[x1,y1,x2,y2]                 | start and end point of a line
INTERSECTS | >5                  | $geoIntersects $geometry type: Polygon    | address.location=ix=[x1,y1 , ...]                 | list of coordinates of a polygon



## FICUM Hazelcast Predicate Visitor

The Hazelcast visitor is capable of traversing a Node tree and converting it to a `com.hazelcast.query.Predicate<K, V>`. The selector names must correspond to the entity `<V>` field names or will be resolved against the visitors `selectorToFieldMapping` Map.

### Text with Wildcards

Text arguments can contain wildcards:
* `?` is a placeholder for one character
* `*` is a placeholder for zero or more characters

When a Test contains a wildcard the comparsion is changed from `EQUALS` to `LIKE` and from `NOT EQUALS` to `NOT LIKE`.



## FICUM Query Printer Visitor

The QueryPrinterVisitor is capable of printing out a FICUM query as string. The FICUM Types are handled as arguments in the following ways:

* Boolean, Byte, Short, Integer, Float - value from toString()
* Long - value from toString() suffixed with `l`
* Double - value from toString() suffixed with `d`
* JodaTime's ReadablePartial or Date and Calendar at midnight - value formated as `yyyy-MM-dd`
* JodaTime's ReadableInstant or Date and Calendar not at midnight - value formated as `yyyy-MM-dd'T'HH:mm:ss.SSSZZ`
* Enum - value from name() surrounded with single quotes
* String, Character and any other Comparable - value from toString() surrounded with single quotes
* Array of previous types - all values as described above enclosed in square brackets and separated by commas, e.g. `[12.5,4.5]`


## The complete [ABNF](https://en.wikipedia.org/wiki/Augmented_Backus%E2%80%93Naur_Form)

All string matches are case-sensitive.

```
expression     =  [ "(" ]
                  ( constraint / expression )
                  [ operator ( constraint / expression ) ]
                  [ ")" ]
operator       = "," / ";" / "." / ":"
constraint     =  selector comparison ( argument / args-array )
selector       =  1*selector-char
                  [ 1*( "." 1*selector-char ) ]
selector-char  =  ALPHA / DIGIT / "_"
comparison     =  "==" / "!=" / "=ge=" / "=le=" / "=gt=" / "=lt=" / "=nr=" / "=wi=" / "=ix="
args-array     =  "[" argument *( "," argument ) "]"
argument       =  date-arg / boolean-arg / null-arg / number-arg / text-arg
date-arg       =  date / dateTime ; as defined in ISO 8601 with yyyy-MM-dd'T'HH:mm:ss.SSSZZ
boolean-arg    =  "yes" / "no" / "true" / "false" / "Yes" / "No" / "True" / "False"
null-arg       =  "null" / "Null"
text-arg       =  ( "'"  ) *( pct-encoded / hex-encoded / CHAR ) ( "'"  )
pct-encoded    =  "%" HEXDIG HEXDIG
hex-encoded    =  ( "#" / "0x" / "0X") 1*( HEXDIG HEXDIG )
number-arg     =  [ "+" / "-" ]
                  ( integer-arg / long-arg / float-arg / double-arg )
integer-arg    =  1*DIGIT
long-arg       =  1*DIGIT ( "l" / "L")
float-arg      =  ( 1*DIGIT "." 1*DIGIT ( "f" / "F" ))
                  [ exponent ]
double-arg     =  ( 1*DIGIT "." 1*DIGIT )
                  [ "d" / "D" ]
                  [ exponent ]
exponent       =  ( "e" / "E" )
                  [ "+" / "-" ]
                  1*DIGIT
```
