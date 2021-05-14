/**
 * @author Nikolaus Knop
 */

package krobot.ast

import krobot.api.ImportsCollector
import krobot.ast.PropertyInitializer.None
import krobot.impl.IndentedWriter

public sealed interface BlockElement : Element

public sealed interface Declaration : BlockElement

internal sealed class PropertyInitializer : Element {
    object None : PropertyInitializer() {
        override fun append(out: IndentedWriter) {}
    }

    data class Value(val value: Expr) : PropertyInitializer() {
        override fun append(out: IndentedWriter) = with(out) {
            append(" = ")
            append(value)
        }
    }

    data class Delegated(val delegate: Expr) : PropertyInitializer() {
        override fun append(out: IndentedWriter) = with(out) {
            append(" by ")
            append(delegate)
        }
    }
}

internal fun IndentedWriter.block(elements: List<Element>) {
    appendLine("{")
    indented {
        join(elements, NewLine)
    }
    appendLine()
    append("}")
}


public sealed interface FunctionBody : Element {
    public data class SingleExpr @PublishedApi internal constructor(private val expr: Expr) : FunctionBody {
        override fun append(out: IndentedWriter): Unit = with(out) {
            append(" = ")
            append(expr)
        }
    }

    public data class Block @PublishedApi internal constructor(private val body: Body) : FunctionBody {
        override fun append(out: IndentedWriter): Unit = out.block(body.statements)
    }
}

internal sealed interface Modifier : Element

internal data class KeywordModifier(private val keyword: String) : Modifier {
    override fun append(out: IndentedWriter) = with(out) {
        append(keyword)
    }
}

internal data class AnnotationModifier(private val clazz: String, private val arguments: List<Expr>) : Modifier {
    override fun append(out: IndentedWriter) = with(out) {
        append('@')
        append(clazz)
        join(arguments, ", ", "(", ")")
    }
}

@PublishedApi internal data class Getter(val modifiers: List<Modifier>, val body: FunctionBody?) : Element {
    override fun append(out: IndentedWriter) = with(out) {
        indented {
            join(modifiers, Space, postfix = Space)
            append("get")
            if (body != null) {
                append("()")
                append(body)
            }
        }
    }
}

@PublishedApi internal data class Setter(
    val modifiers: List<Modifier>,
    val parameterName: String,
    val body: FunctionBody?
) : Element {
    override fun append(out: IndentedWriter) = with(out) {
        indented {
            join(modifiers, Space, postfix = Space)
            append("set")
            if (body != null) {
                append("()")
                append(body)
            }
        }
    }
}

public open class BasicProperty internal constructor(
    @PublishedApi internal val imports: ImportsCollector,
    private val modifiers: List<Modifier>,
    private val valOrVar: String,
    private val name: Identifier,
    internal var receiver: Type? = null,
    internal var type: Type? = null,
    internal var initializer: PropertyInitializer = None
) : Declaration {
    override fun append(out: IndentedWriter): Unit = with(out) {
        join(modifiers, Space)
        space()
        append(valOrVar)
        space()
        join(receiver, ".")
        append(name)
        join(": ", type)
        append(initializer)
    }
}

public class AdvancedProperty @PublishedApi internal constructor(
    imports: ImportsCollector,
    modifiers: List<Modifier>,
    valOrVar: String,
    name: Identifier,
    receiver: Type? = null,
    type: Type? = null,
    initializer: PropertyInitializer = None,
    @PublishedApi internal var getter: Getter? = null,
    @PublishedApi internal var setter: Setter? = null
) : BasicProperty(imports, modifiers, valOrVar, name, type, receiver, initializer) {
    override fun append(out: IndentedWriter): Unit = with(out) {
        super.append(out)
        join(NewLine, getter)
        join(NewLine, setter)
    }
}

public data class Parameter @PublishedApi internal constructor(
    private val name: Identifier,
    private val modifiers: List<Modifier> = emptyList(),
    val type: Type? = null,
    val defaultValue: Expr? = null
) : Element {
    override fun append(out: IndentedWriter): Unit = with(out) {
        join(modifiers, Space, postfix = Space)
        append(name)
        join(": ", type)
        join(Space, "=", Space, defaultValue)
    }
}

public data class TypeParameter internal constructor(
    private val variance: Variance,
    private val name: Identifier,
    private var lowerBound: Type?
) : Element {
    override fun append(out: IndentedWriter): Unit = with(out) {
        append(variance)
        append(name)
        join(": ", lowerBound)
    }
}

public data class Fun @PublishedApi internal constructor(
    internal val imports: ImportsCollector,
    private val name: Identifier,
    private val modifiers: List<Modifier>,
    private val typeParameters: List<TypeParameter> = emptyList(),
    internal var receiver: Type? = null,
    internal var parameters: List<Parameter> = emptyList(),
    internal var returnType: Type? = null,
    internal var body: FunctionBody? = null
) : Declaration {
    override fun append(out: IndentedWriter): Unit = with(out) {
        join(modifiers, Space, postfix = Space)
        append("fun ")
        join(typeParameters, ", ", "<", "> ")
        join(receiver, ".")
        append(name)
        append("(")
        join(parameters, ", ")
        append(")")
        join(": ", returnType)
        space()
        if (body != null) append(body)
    }
}

public data class Constructor internal constructor(
    @PublishedApi internal val imports: ImportsCollector,
    private val modifiers: List<Modifier>,
    private val parameters: List<Parameter>,
    internal var delegationArguments: List<Expr>?,
    @PublishedApi internal var body: List<BlockElement>?
) : Declaration {
    override fun append(out: IndentedWriter): Unit = with(out) {
        join(modifiers, " ")
        append(" constructor(")
        join(parameters, ", ")
        append(")")
        if (delegationArguments != null) {
            append(" : this(")
            join(delegationArguments, ", ")
            append(")")
        }
        if (body != null) block(body!!)
    }
}

internal data class Supertype(val type: Type, val arguments: List<Expr>?, val delegate: Expr?) : Element {
    override fun append(out: IndentedWriter) = with(out) {
        append(type)
        if (arguments != null) append('(')
        join(arguments, ", ")
        if (arguments != null) append(')')
        join(" by ", delegate)
    }
}

public interface HasPrimaryConstructor: DeclarationType
public interface HasTypeParameters: DeclarationType
public interface CanImplement: DeclarationType
public interface CanExtend: CanImplement

public sealed interface DeclarationType {
    public object Interface: HasTypeParameters, CanImplement {
        override fun toString(): String = "interface"
    }

    public object Class: HasTypeParameters, CanExtend, HasPrimaryConstructor {
        override fun toString(): String = "class"
    }

    public object Object: CanExtend {
        override fun toString(): String = "object"
    }

    public object Enum : CanImplement, HasPrimaryConstructor {
        override fun toString(): String = "enum class"
    }
}


public data class EnumEntry @PublishedApi internal constructor(
    val name: Identifier,
    val arguments: List<Expr>,
    val declarations: List<Declaration>?
): Element {
    override fun append(out: IndentedWriter): Unit = with(out) {
        append(name)
        join(arguments.ifEmpty { null }, prefix = "(", postfix = ")")
        if (declarations != null) block(declarations)
    }
}

public data class ClassDefinition<out T: DeclarationType> internal constructor(
    @PublishedApi internal val imports: ImportsCollector,
    private val modifiers: List<Modifier>,
    private val declarationType: T,
    @PublishedApi internal val name: Identifier,
    internal val typeParameters: MutableList<TypeParameter> = mutableListOf(),
    internal var constructorModifiers: List<Modifier> = emptyList(),
    internal var constructorParameters: List<Parameter> = emptyList(),
    internal val supertypes: MutableList<Supertype> = mutableListOf(),
    @PublishedApi internal var enumEntries: List<EnumEntry>? = null,
    @PublishedApi internal var declarations: List<Declaration>? = null
) : Declaration {
    override fun append(out: IndentedWriter): Unit = with(out) {
        join(modifiers, " ", postfix = " ")
        append(declarationType)
        space()
        append(name)
        join(typeParameters, ", ", "<", ">")
        space()
        join(constructorModifiers, " ", postfix = " constructor")
        join(constructorParameters, ", ", "(", ")")
        join(supertypes, ", ", prefix = ": ")
        space()
        if (enumEntries != null || declarations != null) {
            appendLine("{")
            indented {
                join(enumEntries, separator = NewLine)
                if (declarations != null) {
                    if (enumEntries != null) appendLine(";")
                    join(declarations, separator = NewLine)
                }
            }
            appendLine()
            append("}")
        }
    }
}

internal data class TypeAlias(
    private val modifiers: List<Modifier>,
    private val name: Identifier,
    private val parameters: List<String>,
    private val type: Type
) : Declaration {
    override fun append(out: IndentedWriter) = with(out) {
        join(modifiers, " ", postfix = " ")
        append("typealias ")
        append(name)
        join(parameters, ", ", "<", ">")
        append(" = ")
        append(type)
    }
}