[![Build Status](https://travis-ci.org/hschott/ficum.svg)](https://travis-ci.org/hschott/ficum)
# FICUM
## Dynamic Queries - DSL, Parser and Criteria Visitor for Java

Are you tired of writing finder methods for every single use case? Do you have to compile, test and deploy your complete application for just a new finder method?


## FICUM In a Nutshell

FICUM is a simple query language that orientates at [FIQL](https://tools.ietf.org/html/draft-nottingham-atompub-fiql-00), tied together with a Parser and a Criteria Visitor for JPA.

It is inspired by [Apache CXF JAX-RS Search](http://cxf.apache.org/docs/jax-rs-search.html), a blog entry by [Chris Koele](http://koelec.blogspot.de/2012/06/filter-expressions-in-rest-urls.html) and [rsql-parser](https://github.com/jirutka/rsql-parser).

### How to use it

```java
// define selector names allowed to be used in query string
String[] allowedSelectorNames = { "owner", "type", "city" };

// define the query
String input = "owner.city=='Madison',type=='dog'";
// and parse the query into a node tree
Node root = ParseHelper.parse(input, allowedSelectorNames);

// run the JPA visitor on the node tree
JPATypedQueryVisitor<Pet> visitor = new JPATypedQueryVisitor<Pet>(Pet.class);
TypedQuery<Pet> query = visitor.start(root);

// and finally get a list of queried entities
List<Pet> results = query.getResultList();
```

The query string could also passed in via RESTful query `/pets?q=owner.city%3D%3D'Madison'%2Ctype%3D%3D'dog'`.

### Builder

It is also possible to build the node tree and from the node tree a query string.
The Builder works in infix notation as you would write the query as string.

```java
Node root = Builder.newInstance().constraint("owner.city", Comparison.EQUALS, "Madison").and()
        .constraint("type", Comparison.EQUALS, "dog").build();
String query = new QueryPrinterVisitor().start(root);
```



## FICUM Query Language

A FICUM query's input is a string of Unicode characters in the form of an expression.

An expression is composed of one or more constraints, related to each other with Boolean operators. Expressions yield Boolean values: True or False.

```
expression  = [ "(" ]
              ( constraint / expression )
              [ operator ( constraint / expression ) ]
              [ ")" ]
operator    = ";" / ","
```

* `,` is the Boolean AND operator; it yields True if both operands evaluate to True, otherwise False.
* `;` is the Boolean OR operator; it yields True if either operand evaluates to True, otherwise False.

By default, the AND operator takes precedence (i.e., it is evaluated before any OR operators are). However, a parenthesised expression can be used to change precedence, yielding whatever the contained expression yields.

A constraint is composed of a selector, comparison and argument triple, which refines the constraint. When processed, a constraint yields a Boolean value.

```
constraint     =  selector comparison argument
```

A selector identifies the field of an entity that a constraint applies to. Since entities can be nested, also a selector can be defined as nested fields with a dot as seperator.

```
selector       =  1*selector-char
                  [ 1*( "." 1*selector-char ) ]
selector-char  =  ALPHA / DIGIT / "_"
```

A comparsion yields to True if the argument can be processed against the selector defined entity field's value in the following manner:

comparsion | operator
------ | ------
==   | EQUALS
!=   | NOT EQUALS
=ge= | GREATER EQUALS
=le= | LESS EQUALS
=gt= | GREATER THAN
=lt= | LESS THEN

```
comparison     =  "==" / "!=" / "=ge=" / "=le=" / "=gt=" / "=lt="
```

A argument can be of 5 main types. Text, Datetime, Number, Boolean and Null.

```
argument       =  text-arg / date-arg / number-arg / boolean-arg / null-arg
```

*Examples:*
```
firstname==Jack
birthdate==2015-12-24
lastupdate=le=2013-01-04T09:15:00.000+01:00
(points=gt=120;points=le=120),lastplayed=lt=2015-06-05
```


## FICUM Types

The argument's type is negotiated from it's content by a few rules. 

### Date

A date or timestamp is parsed from ISO 8601 string representation and results in a `java.util.Date` object.

A simple date without time will be parsed from the format `yyyy-MM-dd`.

A timestamp will be parsed from the format `yyyy-MM-dd'T'HH:mm:ss.SSSZZ`. The timezone offset value can be either `Z` for UTC or a time value in negative or positive hours, minutes and optional seconds.

*Examples:*
```
2015-12-24 evaluates to 24. December 2015
-645-04-13 evaluates to 13. April 645 BC

2013-01-04T09:15:00.000+01:00 evaluates to 04. January 2013 09:15 AM CET
1492-08-03T15:30:00.000Z evaluates to 03. August 1492 15:30 PM UTC
```

### Number

A number is parsed from a string literal containing digits, sign, decimal dot, qualifier and exponent and results in either `java.lang.Integer`, `java.lang.Long`, `java.lang.Float` or `java.lang.Double`.

*Examples:*

literal | type | value
------ | ------ | ------
23 | Integer | 23
856l | Long | 856
73L | Long | 73
34.01 | Float | 34.01
912.24f | Float | 912.24
2.345F | Float | 2.345
5.5d | Double | 5.5
67.0D | Double | 67.0
210.12E+1 | Float | 2101.2
34.78e-1d | Double | 3.478

### Boolean

A boolean is parsed from a string literal equal to "true" or "false" or "yes" or "no" and results in a `java.lang.Boolean` object.

### Null

A null reference is parsed from a string literal equal to "null".

### Text

A text is parsed from single quoted pct or hex encoded string literals or any string literal that does not match the previous rules and results in a `java.lang.String` object.

Pct encoded strings must start with `%` followed by two hex digits. Hex encoded strings must start with one of `#`, `0x` or `0X` followed by two or more (even number) hex digits. Both can evaluate to a multibyte char.

*Examples:*

literal | value
------ | ------
'%24' | $
'%D4%A2' | Ԣ
'#d4b1' | Ա
'0XD58C' | Ռ
'Hello%20world' | Hello world



## FICUM JPA Visitor

The JPA visitor is capable of traversing a Node tree and converting it to a TypedQuery. The selector names must correspond to the entity field names. 

### Text with Wildcards

Text arguments can contain wildcards:
* `?` is a placeholder for one character
* `*` is a placeholder for zero or more characters

When a Test contains a wildcard the comparsion is changed from `EQUALS` to `LIKE` and from `NOT EQUALS` to `NOT LIKE`.

### Collection size check

When the selector name matches a `java.util.Collection` and the argument is an `java.lang.Integer` the collections size is compared against the argument.


## The complete [ABNF](https://en.wikipedia.org/wiki/Augmented_Backus%E2%80%93Naur_Form)

```
expression     =  [ "(" ]
                  ( constraint / expression )
                  [ operator ( constraint / expression ) ]
                  [ ")" ]
operator       =  ";" / ","
constraint     =  selector comparison argument
selector       =  1*selector-char
                  [ 1*( "." 1*selector-char ) ]
selector-char  =  ALPHA / DIGIT / "_"
comparison     =  "==" / "!=" / "=ge=" / "=le=" / "=gt=" / "=lt="
argument       =  date-arg / boolean-arg / null-arg / number-arg / text-arg
date-arg       =  date / dateTime ; as defined in ISO 8601 with yyyy-MM-dd'T'HH:mm:ss.SSSZZ
boolean-arg    =  "yes" / "no" / "true" / "false" / "Yes" / "No" / "True" / "False"
null-arg       =  "null" / "Null"
text-arg       =  ( "'"  ) 1*( pct-encoded / hex-encoded / CHAR ) ( "'"  )
pct-encoded    =  "%" HEXDIG HEXDIG
hex-encoded    =  ( "#" / "0x" / "0X") 1*( HEXDIG HEXDIG )
number-arg     =  [ "+" / "-" ]
                  ( integer-arg / long-arg / float-arg / double-arg )
integer-arg    =  1*DIGIT
long-arg       =  1*DIGIT ( "l" / "L")
float-arg      =  ( 1*DIGIT "." 1*DIGIT )
                  [ "f" / "F" ]
                  [ exponent ]
double-arg     =  ( 1*DIGIT "." 1*DIGIT ( "d" / "D" ) )
                  [ exponent ]
exponent       =  ( "e" / "E" )
                  [ "+" / "-" ]
                  1*DIGIT
```
