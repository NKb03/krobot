# KRobot - DSL-based Kotlin code generation

KRobot helps you with generating Kotlin files programmatically, for example in annotation processors. What
differentiates KRobot from other existing Kotlin code generation libraries is, that it uses Kotlin's capabilities for
building domain specific languages wherever possible. The API tries to add as little noise to your code as possible, so
you can concentrate on what really matters. This can make *meta*-code look almost like the files that are being
generated. For example the following...

````kotlin
kotlinFile {
    `package`("com.example")
    internal.`object`("Main").body {
        `@`("JvmStatic").`fun`("main", "args" of "Array<String>").body {
            `val`("msg") initializedWith `when`("args".e.select("size")) {
                lit(0) then lit("This is not possible...")
                lit(1) then lit("Oh no, you provided no arguments")
                `in`(lit(2)..lit(5)) then lit("The number of arguments is ok")
                `else` then lit("Too many arguments")
            }
        }
    }
}.saveTo(File("example.kt"))

````

...writes this Kotlin code to the file `example.kt`.

```kotlin
package com.example

internal object Main {
    @JvmStatic
    fun main(args: Array<String>) {
        val msg = when (args.size) {
            0 -> "This is not possible..."
            1 -> "Oh no, you provided no arguments"
            in 2..5 -> "The number of arguments is ok"
            else -> "Too many arguments"
        }
    }
}
```

## Using the library

KRobot is available from Maven Central.

### Gradle

```groovy
dependencies {
    implementation 'com.github.nkb03:krobot:1.0'
}
```

### Maven

```xml
<dependency>
    <groupId>com.github.nkb03</groupId>
    <artifactId>krobot</artifactId>
    <version>1.0</version>
</dependency>
```

## Introductory tutorial

The main entry into the world of KRobot is the function `kotlinFile` from the package `krobot.api`.
(In general, all the declarations that are necessary for ordinary use of the library are located in the `krobot.api`
-package. The `krobot.ast`-package contains the underlying data structures used by the API, which may be needed to be
explicitly accessed when extending the API in client code.)
Inside the closure, that is accepted by the `kotlinFile`-function, you can add the declarations, you want to add to the
file. It then produces an instance of the `KotlinFile`-class containing in AST-form all the declarations that were added
in the closure, which can be saved on the disk using on of the `saveTo`-functions.
A small example:
```kotlin
kotlinFile {
    `package`("crazy.maths")
    `fun`("square", "x" of "Int") returnType "Int" returns "x".e * "x".e 
}.saveTo(File("generatedMaths.kt"))
```
A useful utility are the `saveToSourceRoot`-functions, which take the package declaration of a file into consideration.
For example 
```kotlin
kotlinFile { 
    `package`("foo.bar.baz")
    //declarations...
}.saveToSourceRoot(File("build/generated-src"), "foo.kt")
```
Puts the generated file `foo.kt` into the directory `build/generated/foo/bar/baz`.

Until I have written more documentation on the individual features, 
this fairly extensive example should serve the purpose of introducing you to the library.
```kotlin
import krobot.api.*

fun main() {
    val f = kotlinFile {
        import("kotlin.random.Random")
        `package`("foo.bar")
        abstract.`class`("ExampleClass", `in`("T"))
            .primaryConstructor(
                `@`("PublishedApi").internal,
                private.`val`.parameter("wrapped") of type("List", "Int")
            )
            .implements(type("List", "Int"), by = get("wrapped"))
            .extends("Any", emptyList()) body {
            inline.`fun`(
                listOf(invariant("T") lowerBound "Any"),
                "f",
                "x" of "Int" default lit(3),
                "l" of type("List", "Int"),
                crossinline.parameter("block") of import<java.awt.Robot>().functionType(
                    type("Int"),
                    returnType = type("Int")
                )
            ) returnType "Int" body {
                +call("println", get("x"))
                +`if`(get("x") eq lit(3)).then {
                    +"println"(lit("default value supplied"))
                    +"println"(lit("test"))
                }.`else` {
                    +"println"(lit("value of \$x supplied"))
                    +"println"(lit("test"))
                }
                +"require"(get("l").call("sum") + get("x") less lit(10), closure { +lit("error") })
                +`when` {
                    get("x") eq lit(1) then {
                        +"println"(lit(1))
                    }
                    `else` {
                        +"println"(lit(2))
                    }
                }
                +`when`(get("x")) {
                    `is`("Int") then "println"(lit("is integer"))
                    `in`(`this`("ExampleClass")) then "println"(lit("is in collection"))
                    lit(3) then "println"(lit("is three"))
                    `else` {
                        +"println"(lit("hurray"))
                    }
                }
            }
            private.constructor("test" of "Int").delegate("listOf"("test".e, "Random".e.call("nextInt")))
            abstract.`fun`("f") returnType "Int"
            public.`class`("Inner")
        }
    }
    println(f.pretty())
}
```
It generates the following Kotlin file:
```kotlin
package foo.bar
import kotlin.random.Random
import java.awt.Robot
abstract class ExampleClass<in T> @PublishedApi internal constructor(private val wrapped: List<Int>): List<Int> by wrapped, Any() {
    inline fun <T: Any> Int.f(x: Int = 3, l: List<Int>, crossinline block: Robot.(Int) -> Int) {
        println(x)
        if(x == 3) {
            println("default value supplied")
            println("test")
        } else {
            println("value of $x supplied")
            println("test")
        }
        require(l.sum() + x < 10) { "error" }
        when {
            x == 1 -> println(1)
            else -> println(2)
        }
        when(x) {
            is Int -> println("is integer")
            in this@ExampleClass -> println("is in collection")
            3 -> println("is three")
            else -> println("hurray")
        }
    }
    private constructor(test: Int) : this(listOf(test, Random.nextInt()))
    abstract fun Int.f()
    public class Inner
}
```
If you are unsure how to generate a specific language construct, you can create an Issue on GitHub.

## Contributing

Contributions are greatly appreciated. You can contribute by...

- ...using it in your project, asking questions, reporting bugs, and suggesting features.
- ...implementing new features yourself and making a pull request.
- ...extending the test coverage.
- ...writing documentation for the API in the form of KDoc comments.

If you want to use the project or contribute and have questions, 
please feel free to get help from me - either via email or by creating an issue on GitHub.

### Development setup

Developers who wish to contribute to KRobot are advised to fork the repository and open the project in Intellij. If any
difficulties arise during the project import, feel free to contact me.

## Author

Nikolaus Knop (niko.knop003@gmail.com)

## License

See [LICENSE.MD](LICENSE.md)