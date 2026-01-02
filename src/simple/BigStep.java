package simple;

import java.util.ArrayList;
import java.util.List;

public class BigStep {
    static EnvItem init(Type t){
        if (t == Type.BOOLEAN) {
            return new BoolVal(false);
        }else if(t == Type.INTEGER){
            return new IntVal(0);
        }
        throw new RuntimeException("Invalid Type");
    }
    public static Env evaluateProgram(ProgramNode program){
        Env env = new Env();
        BigStep evaluator = new BigStep();
        for (FuncDef def : program.fns) {
            evaluator.evaluate(def, env);
        }
        for(VarDecl global : program.globals){
            evaluator.evaluate(global, env);
        }
        for (Stmt stmt : program.main) {
            evaluator.evaluate(stmt, env);
        }
        return env;
    } 
    EnvItem evaluate(Expr expr, Env env){
        if(expr instanceof BinaryExpr) return evaluate((BinaryExpr)expr, env);
        if(expr instanceof IntLiteral) return evaluate((IntLiteral)expr, env);
        if(expr instanceof BoolLiteral) return evaluate((BoolLiteral)expr, env);
        if(expr instanceof FuncCall) return evaluate((FuncCall)expr, env);
        if(expr instanceof IdExpr) return evaluate((IdExpr)expr, env);
        throw new UnsupportedOperationException(expr.toString() + "This expression type is not implemented");
    }

    private EnvItem evaluate(IntLiteral expr, Env env){
        return new IntVal(expr.value);
    }
    private EnvItem evaluate(BoolLiteral expr, Env env){
        return new BoolVal(expr.value);
    }
    private EnvItem evaluate(IdExpr expr, Env env){
        return env.getVal(expr.name);
    }
    private EnvItem evaluate(BinaryExpr expr, Env env){
        EnvItem leftItem = evaluate(expr.left, env);
        EnvItem rightItem = evaluate(expr.right, env);
        //to do: add type checking here
        switch (expr.op) {
            case ADD: return new IntVal(((IntVal)leftItem).value + ((IntVal)rightItem).value);
            case AND: return new BoolVal(((BoolVal)leftItem).value && ((BoolVal)rightItem).value);
            case DIV: return new IntVal(((IntVal)leftItem).value / ((IntVal)rightItem).value);
            case EQ: 
                if(leftItem instanceof BoolVal && rightItem instanceof BoolVal)
                    return new BoolVal(((BoolVal)leftItem).value == ((BoolVal)rightItem).value);
                else if(leftItem instanceof IntVal && rightItem instanceof IntVal)
                    return new BoolVal(((IntVal)leftItem).value == ((IntVal)rightItem).value);
                   
            case GT: return new BoolVal(((IntVal)leftItem).value > ((IntVal)rightItem).value);
            case LT:return new BoolVal(((IntVal)leftItem).value < ((IntVal)rightItem).value);
            case MUL: return new IntVal(((IntVal)leftItem).value * ((IntVal)rightItem).value);
            case OR: return new BoolVal(((BoolVal)leftItem).value || ((BoolVal)rightItem).value);
            case SUB: return new IntVal(((IntVal)leftItem).value - ((IntVal)rightItem).value);
            default:
                throw new UnsupportedOperationException("Binary operator not found");
            
        }
    }
    private EnvItem evaluate(FuncCall funcCall, Env env){
        EnvItem func = env.getVal(funcCall.name);
        if(!(func instanceof Closure)){
            throw new RuntimeException(funcCall.name + " is not a function");
        }
        Closure closure = (Closure)func;
        FuncDef funcDef = closure.def;
        if(funcCall.args.size() != funcDef.params.size()){
            throw new RuntimeException("no of Params not matched!");
        }
        Env fenv = closure.funcEnv;
        fenv.enterScope();

        try {
            List<EnvItem> argVals = new ArrayList<>();
            for (Expr argExpr : funcCall.args) {
                argVals.add(evaluate(argExpr, env));
            }
            for (int i = 0; i < argVals.size(); i++) {
                //to do: Add type checking for params here
                fenv.declare(funcDef.params.get(i).name, argVals.get(i));
            }
            
            evaluate(funcDef.body, fenv);

        } catch (ReturnValueException e) {
            return e.value; 
        } finally {
            fenv.exitScope(); 
        }

        // If the function ends without a return statement.
        throw new RuntimeException("Function " + funcCall.name + " did not return a value.");
    }
    void evaluate(Stmt stmt, Env env){
        if(stmt instanceof BlockStmt)evaluate((BlockStmt) stmt, env);
        else if(stmt instanceof VarDecl)evaluate((VarDecl)stmt, env);
        else if(stmt instanceof IfStmt) evaluate((IfStmt) stmt, env);
        else if(stmt instanceof LoopStmt) evaluate((LoopStmt) stmt, env);
        else if(stmt instanceof AssignStmt) evaluate((AssignStmt) stmt, env);
        else if(stmt instanceof ReturnStmt) evaluate((ReturnStmt)stmt, env);
        else throw new UnsupportedOperationException(stmt.toString() + "This statement type has not been implemented");
    }
    void evaluate(FuncDef def, Env env){
        env.declare(def.name, new Closure(def, new Env(env)));
    }
    private void evaluate(VarDecl decl, Env env){
        env.declare(decl.name, BigStep.init(decl.type));
    }
    private void evaluate(BlockStmt b, Env env){
        env.enterScope();
        for (VarDecl decl : b.declarations) {
            evaluate(decl, env);
        }
        for (Stmt stmt : b.statements) {
            evaluate(stmt, env);
        }
        //System.out.println("Exiting Block Env: " + env.toString());
        env.exitScope();
    }
    private void evaluate(IfStmt ifStmt, Env env){
        if(((BoolVal)evaluate(ifStmt.conditional, env)).value){
            evaluate(ifStmt.t, env);
        }else{
            if(ifStmt.e != null)
                evaluate(ifStmt.e, env);
        }
    }
    private void evaluate(LoopStmt loopStmt, Env env){
        while(((BoolVal)evaluate(loopStmt.conditional, env)).value){
            evaluate(loopStmt.body, env);
        }
    }
    private void evaluate(AssignStmt assign, Env env){
        //to do: Add type checking here
        env.addVal(assign.id, evaluate(assign.expr, env));
    }
    private void evaluate(ReturnStmt stmt, Env env){
        //env.exitScope();
        throw new ReturnValueException(evaluate(stmt.expr, env));
    }

}
