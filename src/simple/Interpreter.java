package simple;

import java.util.ArrayList;
import java.util.List;

public class Interpreter {

    // Helper to get default values for typed declarations (int x;)
    private EnvItem defaultFor(Type t) {
        if (t == Type.INTEGER) return new IntVal(0);
        if (t == Type.BOOLEAN) return new BoolVal(false);
        return new UninitializedVal(); // Should not happen for typed decls, but safe fallback
    }

    public void evaluate(AstNode node, Env env) {
        if (node instanceof FuncDef) evaluate((FuncDef) node, env);
        else if (node instanceof Stmt) evaluate((Stmt) node, env);
        else if (node instanceof Expr) {
            EnvItem res = evaluate((Expr) node, env);
            System.out.println("val it = " + res);
        }
    }

    // --- Statements ---

    private void evaluate(Stmt stmt, Env env) {
        if (stmt instanceof BlockStmt) evaluate((BlockStmt) stmt, env);
        else if (stmt instanceof VarDecl) evaluate((VarDecl) stmt, env);
        else if (stmt instanceof IfStmt) evaluate((IfStmt) stmt, env);
        else if (stmt instanceof LoopStmt) evaluate((LoopStmt) stmt, env);
        else if (stmt instanceof AssignStmt) evaluate((AssignStmt) stmt, env);
        else if (stmt instanceof ReturnStmt) evaluate((ReturnStmt) stmt, env);
        else throw new UnsupportedOperationException("Unknown Stmt: " + stmt);
    }

    private void evaluate(VarDecl decl, Env env) {
        // Fix for 'var x': If type is null (inferred), use UninitializedVal
        if (decl.type == null) {
            env.declare(decl.name, new UninitializedVal());
        } else {
            env.declare(decl.name, defaultFor(decl.type));
        }
    }

    private void evaluate(AssignStmt stmt, Env env) {
        EnvItem val = evaluate(stmt.expr, env);
        env.addVal(stmt.id, val);
    }

    private void evaluate(BlockStmt block, Env env) {
        env.enterScope();
        try {
            for (VarDecl d : block.declarations) evaluate(d, env);
            for (Stmt s : block.statements) evaluate(s, env);
        } finally {
            env.exitScope();
        }
    }

    private void evaluate(IfStmt stmt, Env env) {
        BoolVal cond = (BoolVal) evaluate(stmt.conditional, env);
        if (cond.value) evaluate(stmt.t, env);
        else if (stmt.e != null) evaluate(stmt.e, env);
    }

    private void evaluate(LoopStmt stmt, Env env) {
        while (((BoolVal) evaluate(stmt.conditional, env)).value) {
            evaluate(stmt.body, env);
        }
    }

    private void evaluate(ReturnStmt stmt, Env env) {
        throw new ReturnValueException(evaluate(stmt.expr, env));
    }

    private void evaluate(FuncDef def, Env env) {
        env.declare(def.name, new Closure(def, new Env(env)));
        // Note: The REPL App.java prints the type info, so we don't need to print here.
    }

    // --- Expressions ---

    private EnvItem evaluate(Expr expr, Env env) {
        if (expr instanceof IntLiteral) return new IntVal(((IntLiteral) expr).value);
        if (expr instanceof BoolLiteral) return new BoolVal(((BoolLiteral) expr).value);
        if (expr instanceof IdExpr) return env.getVal(((IdExpr) expr).name);
        if (expr instanceof BinaryExpr) return evaluate((BinaryExpr) expr, env);
        if (expr instanceof FuncCall) return evaluate((FuncCall) expr, env);
        throw new UnsupportedOperationException("Unknown Expr: " + expr);
    }

    private EnvItem evaluate(BinaryExpr expr, Env env) {
        EnvItem l = evaluate(expr.left, env);
        EnvItem r = evaluate(expr.right, env);
        
        // We assume TypeChecker passed, so strict casting is safe-ish.
        // For production, retain instanceof checks.
        
        switch (expr.op) {
            case ADD: return new IntVal(((IntVal) l).value + ((IntVal) r).value);
            case SUB: return new IntVal(((IntVal) l).value - ((IntVal) r).value);
            case MUL: return new IntVal(((IntVal) l).value * ((IntVal) r).value);
            case DIV: return new IntVal(((IntVal) l).value / ((IntVal) r).value);
            case AND: return new BoolVal(((BoolVal) l).value && ((BoolVal) r).value);
            case OR:  return new BoolVal(((BoolVal) l).value || ((BoolVal) r).value);
            case GT:  return new BoolVal(((IntVal) l).value > ((IntVal) r).value);
            case LT:  return new BoolVal(((IntVal) l).value < ((IntVal) r).value);
            case EQ:
                if (l instanceof IntVal) return new BoolVal(((IntVal) l).value == ((IntVal) r).value);
                return new BoolVal(((BoolVal) l).value == ((BoolVal) r).value);
            default: throw new RuntimeException("Unknown Op: " + expr.op);
        }
    }

    private EnvItem evaluate(FuncCall call, Env env) {
        Closure closure = (Closure) env.getVal(call.name);
        Env funcEnv = closure.funcEnv; // Use captured environment
        
        // Evaluate args in current scope
        List<EnvItem> args = new ArrayList<>();
        for (Expr e : call.args) args.add(evaluate(e, env));

        funcEnv.enterScope();
        try {
            // Bind parameters
            for (int i = 0; i < closure.def.params.size(); i++) {
                funcEnv.declare(closure.def.params.get(i).name, args.get(i));
            }
            evaluate(closure.def.body, funcEnv);
        } catch (ReturnValueException ret) {
            return ret.value;
        } finally {
            funcEnv.exitScope();
        }
        throw new RuntimeException("Function " + call.name + " finished without return.");
    }
}