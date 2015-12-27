## FICUM
### Dynamic Queries - DSL, Parser and Criteria Visitor

Are you tired of writing finder methods for every single use case? Do you have to compile, test and deploy your complete application for just a new finder method?

FICUM is a simple query language that orientates at [FIQL](https://tools.ietf.org/html/draft-nottingham-atompub-fiql-00), tied together with a Parser and a Criteria Visitor.

FICUM is inspired by [Apache CXF JAX-RS Search](http://cxf.apache.org/docs/jax-rs-search.html), [a blog entry by Chris Koele](http://koelec.blogspot.de/2012/06/filter-expressions-in-rest-urls.html) and [rsql-parser](https://github.com/jirutka/rsql-parser).



### FICUM Query Language

A FICUM query's input is a string of Unicode characters in the form of an expression.

An FICUM expression is composed of one or more constraints, related to each other with Boolean operators.

FICUM expressions yield Boolean values: True or False.

```
expression  = [ "(" ]
              ( constraint / expression )
              [ operator ( constraint / expression ) ]
              [ ")" ]
operator    = ";" / ","
```

* ";" is the Boolean AND operator; it yields True for a particular entry if both operands evaluate to True, otherwise False.
* "," is the Boolean OR operator; it yields True if either operand evaluates to True, otherwise False.

By default, the AND operator takes precedence (i.e., it is evaluated before any OR operators are). However, a parenthesised expression can be used to change precedence, yielding whatever the contained expression yields.

A FICUM constraint is composed of a selector, comparison and argument triple, which refines the constraint. When processed, a constraint yields a Boolean value.

```
constraint     =  selector comparison argument
```

A FICUM selector identifies the field of an entity that a constraint applies to. Since entities can be nested, also a selector can be defined as nested fields with a dot as seperator.

```
selector       =  1*selector-char
                  [ 1*( "." 1*selector-char ) ]
selector-char  =  ALPHA / DIGIT / "_"
```

A FICUM comparsion yields to True if the argument can be processed against the selector defined entity field's value in the following manner:

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

A FICUM argument can be of 5 main types. Text, Datetime, Number, Boolean and Null.

```
argument       =  text-arg / date-arg / number-arg / boolean-arg
```

### FICUM Types

TODO


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
argument       =  text-arg / date-arg / number-arg / boolean-arg
text-arg       =  1*( pct-encoded / hex-encoded / quoted-char )
pct-encoded    =  "%" HEXDIG HEXDIG
hex-encoded    =  ( "#" / "0x" / "0X") 1*( HEXDIG HEXDIG )
quoted-char    =  ( "'" / DQUOTE ) 1*( CHAR ) ( "'" / DQUOTE )
date-arg       =  date / timestamp ; as defined in ISO 8601 with yyyy-MM-dd'T'HH:mm:ss.SSSZZ
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
