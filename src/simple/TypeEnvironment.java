package simple;

import java.util.Deque;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

public class TypeEnvironment {
    private final Deque<Map<String, TypeExpr>> scopeStack;
    public TypeEnvironment(){
        this.scopeStack = new LinkedList<>();
        enterScope();
    }
    
    public void enterScope(){
        scopeStack.push(new HashMap<>());
    }
    public void exitScope(){
        scopeStack.pop();
    }
    public TypeExpr lookup(String key){
        for (Map<String,TypeExpr> sigmaMap : scopeStack) {
            if(sigmaMap.containsKey(key)){
                return sigmaMap.get(key);
            }
        }
        throw new RuntimeException("Variable named " + key + " Not declared in this program");
    }
    public void addVal(String name, TypeExpr value){
        for (Map<String,TypeExpr> sigmaMap : scopeStack) {
            if(sigmaMap.containsKey(name)){
                sigmaMap.put(name, value);
                return;
            }
        }
        throw new RuntimeException("Variable named " + name + " Not declared in this program");

    }
    public void declare(String name, TypeExpr value){
        if (scopeStack.isEmpty()) {
            throw new IllegalStateException("No scope is active.");
        }
        // Throws an error if the variable is already declared in the current scope.
        if (scopeStack.peek().containsKey(name)) {
            throw new RuntimeException("Variable '" + name + "' is already defined in this scope.");
        }
        scopeStack.peek().put(name, value);
    }
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("{\n");
        // Only prints the outermost (global) scope
        if (!scopeStack.isEmpty()) {
            Map<String, TypeExpr> globalScope = scopeStack.getLast();
            for (Map.Entry<String, TypeExpr> entry : globalScope.entrySet()) {
                sb.append("  ").append(entry.getKey()).append(" = ").append(entry.getValue().toString()).append("\n");
            }
        }
        sb.append("}");
        return sb.toString();
    }
}
