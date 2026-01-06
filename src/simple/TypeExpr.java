package simple;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public abstract class TypeExpr{
    public TypeExpr find(){return this;}
}
class TypeConst extends TypeExpr{
    final Type type;
    
    public TypeConst(Type type) {
        this.type = type;
    }

    @Override
    public String toString() {
        return type.toString();
    }
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        TypeConst typeConst = (TypeConst) o;
        return type.equals(typeConst.type);
    }

    @Override
    public int hashCode() { return Objects.hash(type); }
    
}
class TypeVar extends TypeExpr{
    private static int nextId = 0;
    final int id;
    TypeExpr instance = null;
    public TypeVar(){
        this.id = nextId++;
    }
    @Override
    public TypeExpr find() {
        if (instance != null) {
            TypeExpr root = instance.find();
            instance = root;
            return root;
        }
        return this; // This TypeVar is its own leader for now.
    }
    @Override
    public String toString() {
        if(instance != null)
            return instance.toString();
        return "t: " + String.valueOf(id);
    }
}
class FuncType extends TypeExpr{
    final TypeExpr returnType;
    final List<TypeExpr> paramTypes;
    public FuncType(TypeExpr returnType, List<TypeExpr> paramTypes) {
        this.returnType = returnType;
        this.paramTypes = paramTypes;
    }
    @Override
    public String toString() {
        // Use a stream to convert each parameter's TypeExpr to its string representation.
        String paramsStr = paramTypes.stream()
                                     .map(TypeExpr::toString) // Recursively calls toString on each type
                                     .collect(Collectors.joining(", "));

        // Combine the parameter string and the return type string into the final format.
        return String.format("(%s) -> %s", paramsStr, returnType.toString());
    }
    
}