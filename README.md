## FICUM
### Dynamic Queries - DSL, Parser and Criteria Visitor for Java

Are you tired of writing finder methods for every single use case? Do you have to compile, test and deploy your complete application for just a new finder method?

FICUM is a simple query language that orientates at [FIQL](https://tools.ietf.org/html/draft-nottingham-atompub-fiql-00), tied together with a Parser and a Criteria Visitor.

It is inspired by [Apache CXF JAX-RS Search](http://cxf.apache.org/docs/jax-rs-search.html), a blog entry by [Chris Koele](http://koelec.blogspot.de/2012/06/filter-expressions-in-rest-urls.html) and [rsql-parser](https://github.com/jirutka/rsql-parser).



### FICUM Query Language

A FICUM query's input is a string of Unicode characters in the form of an expression.

An expression is composed of one or more constraints, related to each other with Boolean operators. Expressions yield Boolean values: True or False.

```
expression  = [ "(" ]
              ( constraint / expression )
              [ operator ( constraint / expression ) ]
              [ ")" ]
operator    = ";" / ","
```

* `;` is the Boolean AND operator; it yields True for a particular entry if both operands evaluate to True, otherwise False.
* `,` is the Boolean OR operator; it yields True if either operand evaluates to True, otherwise False.

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

### FICUM Types

The argument's type is negotiated from it's content by a few rules. 

#### Date

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

#### Number

A number is parsed from a string literal containing digits, sign, decimal dot, qualifier and exponent and results in either `java.lang.Integer`, `java.lang.Long`, `java.lang.Float` or `java.lang.Double`.

*Examples:*

Literal | Type | Value
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

#### Text

#### Boolean

A boolean is parsed from a string literal containing "true" or "false" or "yes" or "no" and results in a `java.lang.Boolean` object.

#### Null

A null reference is parsed from a string literal containing "null".


#### The complete [ABNF](https://en.wikipedia.org/wiki/Augmented_Backus%E2%80%93Naur_Form)

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
text-arg       =  1*( pct-encoded / hex-encoded / quoted-char )
pct-encoded    =  "%" HEXDIG HEXDIG
hex-encoded    =  ( "#" / "0x" / "0X") 1*( HEXDIG HEXDIG )
quoted-char    =  ( "'" / DQUOTE ) 1*( CHAR ) ( "'" / DQUOTE )
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
