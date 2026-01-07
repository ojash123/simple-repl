package simple;

import java.util.Deque;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;

abstract class EnvItem{}

class IntVal extends EnvItem{
    final int value;

    public IntVal(int value) {
        this.value = value;
    }
    public String toString(){
        return String.valueOf(value);
    }
}
class BoolVal extends EnvItem{
    final boolean value;

    public BoolVal(boolean value) {
        this.value = value;
    }
    public String toString(){
        return String.valueOf(value);
    }
}

// [NEW] Placeholder for 'var x;' declarations
class UninitializedVal extends EnvItem {
    public String toString() { return "Uninitialized"; }
}

class Closure extends EnvItem{
    final FuncDef def;
    final Env funcEnv;
    public Closure(FuncDef def, Env funcEnv) {
        this.def = def;
        this.funcEnv = funcEnv;
    }
    public String toString(){
        return "function: " + def.name;
    }
} 
// Custom exception for handling 'return' control flow.
class ReturnValueException extends RuntimeException {
    final EnvItem value;
    public ReturnValueException(EnvItem val) { this.value = val; }
}  

public class Env{
    private final Deque<Map<String, EnvItem>> scopeStack;
    public Env(){
        this.scopeStack = new LinkedList<>();
        enterScope();
    }
    //copy constructor for Function environment
    public Env(Env other) {
        this.scopeStack = new LinkedList<>();
        for (Map<String, EnvItem> scope : other.scopeStack) {
            this.scopeStack.addLast(new HashMap<>(scope));
        }
    }
    public void enterScope(){
        scopeStack.push(new HashMap<>());
    }
    public void exitScope(){
        scopeStack.pop();
    }
    public EnvItem getVal(String key){
        for (Map<String,EnvItem> sigmaMap : scopeStack) {
            if(sigmaMap.containsKey(key)){
                return sigmaMap.get(key);
            }
        }
        throw new RuntimeException("Variable named " + key + " Not declared in this program");
    }
    public void addVal(String name, EnvItem value){
        for (Map<String,EnvItem> sigmaMap : scopeStack) {
            if(sigmaMap.containsKey(name)){
                sigmaMap.put(name, value);
                return;
            }
        }
        throw new RuntimeException("Variable named " + name + " Not declared in this program");

    }
    public void declare(String name, EnvItem value){
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
            Map<String, EnvItem> globalScope = scopeStack.getLast();
            for (Map.Entry<String, EnvItem> entry : globalScope.entrySet()) {
                sb.append("  ").append(entry.getKey()).append(" = ").append(entry.getValue().toString()).append("\n");
            }
        }
        sb.append("}");
        return sb.toString();
    }
}

