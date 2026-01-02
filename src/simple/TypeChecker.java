package simple;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TypeChecker {
    private TypeEnvironment typeEnv; //Symbol table
    private static final TypeConst INT_TYPE = new TypeConst(Type.INTEGER);
    private static final TypeConst BOOL_TYPE = new TypeConst(Type.BOOLEAN);
    private TypeExpr currentFunctionReturnType;

    public void typeCheckProgram(ProgramNode program) throws Exception {
        this.typeEnv = new TypeEnvironment();

        for (FuncDef funcDef : program.fns) {
            registerFunctionSignature(funcDef);
        }

        for (FuncDef funcDef : program.fns) {
            checkFuncDef(funcDef);
        }

        this.currentFunctionReturnType = null; // No return statements allowed in main
        for(VarDecl decl : program.globals){
            checkStmt(decl);
        }
        for (Stmt stmt : program.main) {
            checkStmt(stmt);
        }
        System.out.println(typeEnv.toString());
    }


    private void registerFunctionSignature(FuncDef funcDef) throws Exception {
        List<TypeExpr> paramTypes = new ArrayList<>();
        for (VarDecl param : funcDef.params) {
            if (param.type == null) { // Implicitly typed param: var x
                paramTypes.add(new TypeVar());
            } else { // Explicitly typed param: int x
                paramTypes.add(typeFromAst(param.type));
            }
        }
        TypeVar returnType = new TypeVar();
        FuncType funcType = new FuncType(returnType, paramTypes);
        typeEnv.declare(funcDef.name, funcType);
    }



    private void checkFuncDef(FuncDef funcDef) throws Exception {
        FuncType funcType = (FuncType) typeEnv.lookup(funcDef.name).find();

        this.currentFunctionReturnType = funcType.returnType;

        typeEnv.enterScope();
        try {
            for (int i = 0; i < funcDef.params.size(); i++) {
                VarDecl param = funcDef.params.get(i);
                TypeExpr paramType = funcType.paramTypes.get(i);
                typeEnv.declare(param.name, paramType);
            }
            checkStmt(funcDef.body);
        } finally {
            //exit scope, no longer expecting return statements
            typeEnv.exitScope();
            currentFunctionReturnType = null;
        }
    }

    private void checkStmt(Stmt stmt) throws Exception {
        if (stmt instanceof VarDecl) {
            VarDecl decl = (VarDecl) stmt;
            TypeExpr declaredType;
            if (decl.type == null) { // var x;
                declaredType = new TypeVar();
            } else { // int x;
                declaredType = typeFromAst(decl.type);
            }
            typeEnv.declare(decl.name, declaredType);
        } else if (stmt instanceof AssignStmt) {
            AssignStmt assign = (AssignStmt) stmt;
            TypeExpr varType = typeEnv.lookup(assign.id);
            TypeExpr exprType = checkExpr(assign.expr);
            unify(varType, exprType);
        } else if (stmt instanceof BlockStmt) {
            BlockStmt block = (BlockStmt) stmt;
            typeEnv.enterScope();
            try {
                for (VarDecl decl : block.declarations) {
                    checkStmt(decl);
                }
                for (Stmt s : block.statements) {
                    checkStmt(s);
                }
            } finally {
                typeEnv.exitScope();
            }
        } else if (stmt instanceof IfStmt) {
            IfStmt ifStmt = (IfStmt) stmt;
            TypeExpr conditionType = checkExpr(ifStmt.conditional);
            unify(conditionType, BOOL_TYPE);
            checkStmt(ifStmt.t);
            if (ifStmt.e != null) {
                checkStmt(ifStmt.e);
            }
        } else if (stmt instanceof LoopStmt) {
            LoopStmt loop = (LoopStmt) stmt;
            TypeExpr conditionType = checkExpr(loop.conditional);
            unify(conditionType, BOOL_TYPE);
            checkStmt(loop.body);
        } else if (stmt instanceof ReturnStmt) {
            ReturnStmt ret = (ReturnStmt) stmt;
            if (currentFunctionReturnType == null) {
                throw new Exception("Return statement found outside of a function body.");
            }
            TypeExpr returnExprType = checkExpr(ret.expr);
            unify(returnExprType, currentFunctionReturnType);
        }
    }

    private TypeExpr checkExpr(Expr expr) throws Exception {
        TypeExpr resultType;
        if (expr instanceof IntLiteral) {
            resultType = INT_TYPE;
        } else if (expr instanceof BoolLiteral) {
            resultType = BOOL_TYPE;
        } else if (expr instanceof IdExpr) {
            resultType = typeEnv.lookup(((IdExpr) expr).name);
        } else if (expr instanceof BinaryExpr) {
            resultType = checkBinaryExpr((BinaryExpr) expr);
        } else if (expr instanceof FuncCall) {
            resultType = checkFuncCall((FuncCall) expr);
        } else {
            throw new Exception("Unhandled expression type in type checker.");
        }

        return resultType;
    }

    private TypeExpr checkBinaryExpr(BinaryExpr expr) throws Exception {
        TypeExpr leftType = checkExpr(expr.left);
        TypeExpr rightType = checkExpr(expr.right);
        switch (expr.op) {
            case ADD:
            case SUB:
            case MUL:
            case DIV:
                unify(leftType, INT_TYPE);
                unify(rightType, INT_TYPE);
                return INT_TYPE;
            case LT:
            case GT:
                unify(leftType, INT_TYPE);
                unify(rightType, INT_TYPE);
                return BOOL_TYPE;
            case AND:
            case OR:
                unify(leftType, BOOL_TYPE);
                unify(rightType, BOOL_TYPE);
                return BOOL_TYPE;
            case EQ:
                unify(leftType, rightType); 
                return BOOL_TYPE;
            default:
                throw new Exception("Unknown binary operator.");
        }
    }

    private TypeExpr checkFuncCall(FuncCall call) throws Exception {
        TypeExpr funcTypeRaw = typeEnv.lookup(call.name);
        TypeExpr funcTypeRep = funcTypeRaw.find();

        if (funcTypeRep instanceof TypeVar) {
            List<TypeExpr> argTypes = new ArrayList<>();
            for (Expr arg : call.args) {
                argTypes.add(checkExpr(arg));
            }
            TypeVar returnType = new TypeVar();
            FuncType inferredType = new FuncType(returnType, argTypes);
            unify(funcTypeRaw, inferredType);
            return returnType;
        } else if (funcTypeRep instanceof FuncType) {
            FuncType funcType = (FuncType) funcTypeRep;
            if (call.args.size() != funcType.paramTypes.size()) {
                throw new Exception("Function arity mismatch: " + call.name);
            }
            for (int i = 0; i < call.args.size(); i++) {
                TypeExpr argType = checkExpr(call.args.get(i));
                TypeExpr paramType = funcType.paramTypes.get(i);
                unify(argType, paramType);
            }
            return funcType.returnType;
        } else {
            throw new Exception("'" + call.name + "' is not a function.");
        }
    }

    void unify(TypeExpr t1, TypeExpr t2) throws Exception{
        TypeExpr rep1 = t1.find(), rep2 = t2.find();
        if(rep1 == rep2){
            //same type
            return;
        }
        if(rep1 instanceof TypeVar){
            ((TypeVar)rep1).instance = rep2;
            return;
        }
        if(rep2 instanceof TypeVar){
            ((TypeVar)rep2).instance = rep1;
            return;
        }
        if(rep1 instanceof TypeConst && rep2 instanceof TypeConst){
            if(!rep1.equals(rep2) ){
                throw new Exception("Type Mismatch: " + rep1.toString() + " " + rep2.toString());
            }
            return;
        }
        if (rep1 instanceof FuncType && rep2 instanceof FuncType) {
            FuncType f1 = (FuncType) rep1;
            FuncType f2 = (FuncType) rep2;

            // Check for the same number of parameters.
            if (f1.paramTypes.size() != f2.paramTypes.size()) {
                throw new Exception("Function arity mismatch: Cannot unify " + f1 + " with " + f2);
            }
            //unify return types
            unify(f1.returnType, f2.returnType);

            //unify each parameter type.
            for (int i = 0; i < f1.paramTypes.size(); i++) {
                unify(f1.paramTypes.get(i), f2.paramTypes.get(i));
            }
            return;
        }
        throw new Exception("Type Mismatch: " + rep1.toString() + " " + rep2.toString());

    }

    private TypeConst typeFromAst(Type astType){
        if(astType == Type.BOOLEAN)return BOOL_TYPE;
        if(astType == Type.INTEGER)return INT_TYPE;
        throw new IllegalArgumentException("Invalid Ast Type");
    }
}
