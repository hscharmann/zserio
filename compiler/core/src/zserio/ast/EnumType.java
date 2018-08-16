package zserio.ast;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

import zserio.antlr.ZserioParserTokenTypes;
import zserio.antlr.util.BaseTokenAST;
import zserio.antlr.util.ParserException;
import zserio.ast.doc.DocCommentToken;

/**
 * AST node for enumeration types.
 *
 * Enumeration types are Zserio types as well.
 */
public class EnumType extends TokenAST implements ZserioType
{
    /**
     * Default constructor.
     */
    public EnumType()
    {
        enumItems = new ArrayList<EnumItem>();
        usedTypeList = new ArrayList<ZserioType>();
        usedByCompoundList = new TreeSet<CompoundType>();
        ZserioTypeContainer.add(this);
    }

    @Override
    public Package getPackage()
    {
        return pkg;
    }

    @Override
    public String getName()
    {
        return name;
    }

    @Override
    public Iterable<ZserioType> getUsedTypeList()
    {
        return usedTypeList;
    }

    @Override
    public void callVisitor(ZserioTypeVisitor visitor)
    {
        visitor.visitEnumType(this);
    }

    /**
     * Gets the scope defined by this type.
     *
     * @return The scope defined by this type.
     */
    public Scope getScope()
    {
        return scope;
    }

    /**
     * Sets lexical scope and package for the enumeration type.
     *
     * This method is called by code generated by ANTLR using TypeEvaluator.g.
     *
     * @param scope Lexical scope to set.
     * @param pkg   Package to set.
     */
    public void setScope(Scope scope, Package pkg)
    {
        this.scope = scope;
        this.pkg = pkg;
    }

    /**
     * Sets compound type which uses this enumeration type.
     *
     * @param compoundType Compound type to set.
     */
    public void setUsedByCompoundType(CompoundType compoundType)
    {
        usedByCompoundList.add(compoundType);
    }

    /**
     * Evaluates all enumeration item values of the enumeration type.
     *
     * This method is called from code generated by ANTLR using ExpressionEvaluator.g grammar as soon as all
     * enumeration items are evaluated.
     *
     * This method can be called directly from Expression.evaluate() method if some expression refers to
     * enumeration item before definition of this item.
     *
     * This method calculates and sets value to all enumeration items.
     *
     * @throws ParserException Throws if evaluation of all enumeration item values fails.
     */
    public void evaluateItemValues() throws ParserException
    {
        if (!areItemValuesEvaluated)
        {
            // evaluate enumeration values
            BigInteger defaultEnumItemValue = BigInteger.ZERO;
            for (EnumItem enumItem : enumItems)
            {
                enumItem.evaluateValue(defaultEnumItemValue);
                defaultEnumItemValue = enumItem.getValue().add(BigInteger.ONE);
            }

            areItemValuesEvaluated = true;
        }
    }

    /**
     * Gets all enumeration items which belong to the enumeration type.
     *
     * @return List of all enumeration items.
     */
    public List<EnumItem> getItems()
    {
        return Collections.unmodifiableList(enumItems);
    }

    /**
     * Gets unresolved enumeration Zserio type.
     *
     * @return Unresolved enumeration Zserio type.
     */
    public ZserioType getEnumType()
    {
        return enumType;
    }

    /**
     * Gets enumeration integer type.
     *
     * @return Enumeration integer type.
     */
    public IntegerType getIntegerBaseType()
    {
        return integerBaseType;
    }

    /**
     * Gets list of compound types which use this enumeration type.
     *
     * @return List of compound types which use this enumeration type.
     */
    public Iterable<CompoundType> getUsedByCompoundList()
    {
        return usedByCompoundList;
    }

    /**
     * Gets the list of referenced Const Types of the enumeration type.
     *
     * @return List of referenced Const Types of the enumeration type.
     */
    public Set<ConstType> getReferencedConstTypes()
    {
        final Set<ConstType> referencedConstTypes = new HashSet<ConstType>();
        if (integerBaseType instanceof BitFieldType)
        {
            final Expression lengthExpression = ((BitFieldType)integerBaseType).getLengthExpression();
            referencedConstTypes.addAll(lengthExpression.getReferencedSymbolObjects(ConstType.class));
        }

        return referencedConstTypes;
    }

    /**
     * Gets documentation comment associated to this enumeration type.
     *
     * @return Documentation comment token associated to this enumeration type.
     */
    public DocCommentToken getDocComment()
    {
        return getHiddenDocComment();
    }

    @Override
    protected boolean evaluateChild(BaseTokenAST child) throws ParserException
    {
        switch (child.getType())
        {
        case ZserioParserTokenTypes.ID:
            name = child.getText();
            break;

        case ZserioParserTokenTypes.ITEM:
            if (!(child instanceof EnumItem))
                return false;
            final EnumItem enumItem = (EnumItem)child;
            enumItem.setEnumType(this);
            enumItems.add(enumItem);
            break;

        default:
            if (enumType != null || !(child instanceof ZserioType))
                return false;
            enumType = (ZserioType)child;
            break;
        }

        return true;
    }

    @Override
    protected void evaluate() throws ParserException
    {
        evaluateHiddenDocComment(this);
    }

    @Override
    protected void check() throws ParserException
    {
        // fill used type list
        final ZserioType resolvedTypeReference = TypeReference.resolveType(enumType);
        if (!ZserioTypeUtil.isBuiltIn(resolvedTypeReference))
            usedTypeList.add(resolvedTypeReference);

        // fill resolved enumeration type
        final ZserioType baseType = TypeReference.resolveBaseType(enumType);
        if (!(baseType instanceof IntegerType))
            throw new ParserException(this, "Enumeration '" + this.getName() + "' has forbidden type " +
                    baseType.getName() + "!");
        integerBaseType = (IntegerType)baseType;

        // check enumeration items
        checkEnumerationItems();
    }

    private void checkEnumerationItems() throws ParserException
    {
        final Set<BigInteger> enumItemValues = new HashSet<BigInteger>();
        for (EnumItem enumItem : enumItems)
        {
            // check if enumeration item value is not duplicated
            final BigInteger enumItemValue = enumItem.getValue();
            if ( !enumItemValues.add(enumItemValue) )
            {
                // enumeration item value is duplicated
                throw new ParserException(enumItem.getValueExpression(), "Enumeration item '" +
                        enumItem.getName() + "' has duplicated value (" + enumItemValue + ")!");
            }

            // check enumeration item values boundaries
            final BigInteger lowerBound = integerBaseType.getLowerBound();
            final BigInteger upperBound = integerBaseType.getUpperBound();
            if (enumItemValue.compareTo(lowerBound) < 0 || enumItemValue.compareTo(upperBound) > 0)
                throw new ParserException(enumItem.getValueExpression(), "Enumeration item '" +
                        enumItem.getName() + "' has value (" + enumItemValue + ") out of range <" +
                        lowerBound + "," + upperBound + ">!");
        }
    }

    private static final long serialVersionUID = -7849005190109895313L;

    private Scope scope;
    private Package pkg;

    private ZserioType enumType = null;
    private String name;
    private List<EnumItem> enumItems;

    private boolean areItemValuesEvaluated = false;
    private List<ZserioType> usedTypeList;
    private IntegerType integerBaseType;
    private SortedSet<CompoundType> usedByCompoundList;
}
