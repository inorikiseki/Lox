package com.interpreter.lox;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Stack;

import com.interpreter.lox.Expr.Assign;
import com.interpreter.lox.Expr.Binary;
import com.interpreter.lox.Expr.Call;
import com.interpreter.lox.Expr.Grouping;
import com.interpreter.lox.Expr.Literal;
import com.interpreter.lox.Expr.Logical;
import com.interpreter.lox.Expr.Unary;
import com.interpreter.lox.Expr.Variable;
import com.interpreter.lox.Stmt.Block;
import com.interpreter.lox.Stmt.Break;
import com.interpreter.lox.Stmt.Expression;
import com.interpreter.lox.Stmt.Function;
import com.interpreter.lox.Stmt.If;
import com.interpreter.lox.Stmt.Print;
import com.interpreter.lox.Stmt.Return;
import com.interpreter.lox.Stmt.Var;
import com.interpreter.lox.Stmt.While;

public class Resolver 
implements Expr.Visitor<Void>, Stmt.Visitor<Void>{
	private final Interpreter interpreter;
	private final Stack<Map<String,Boolean>> scopes = new Stack<>();
	private enum FunctionType{
		NONE,
		FUNCTION
	}
	
	private FunctionType currentFunction = FunctionType.NONE;
	
	Resolver(Interpreter interpreter){
		this.interpreter = interpreter;
	}
	
	void resolve(List<Stmt> statements) {
		for(Stmt statement:statements) {
			resolve(statement);
		}
	}
	@Override
	public Void visitBlockStmt(Block stmt) {
		beginScope();
		resolve(stmt.statements);
		endScope();
		return null;
	}
	
	private void resolve(Stmt stmt) {
		stmt.accept(this);
	}
	
	private void resolve(Expr expr) {
		expr.accept(this);
	}

	
	private void endScope() {
		scopes.pop();
	}

	private void beginScope() {
		scopes.push(new HashMap<String,Boolean>());
	}
	
	private void declare(Token name) {
		if (scopes.isEmpty()) return;
		
		Map<String,Boolean> scope = scopes.peek();
		
		if(scope.containsKey(name.lexeme)) {
			Lox.error(name, 
					"Already a variable with "
					+ "this name in this scope.");
		}
		scope.put(name.lexeme, false);
	}
	
	private void define(Token name) {
		if (scopes.isEmpty()) return;
		scopes.peek().put(name.lexeme,true);
	}

	@Override
	public Void visitExpressionStmt(Expression stmt) {
		resolve(stmt.expression);
		return null;
	}

	@Override
	public Void visitFunctionStmt(Function stmt) {
		declare(stmt.name);
		define(stmt.name);
		resolveFunction(stmt,FunctionType.FUNCTION);
		return null;
	}

	private void resolveFunction(Function function,
			FunctionType type) {
		FunctionType enclosingFunction = currentFunction;
		currentFunction = type;
		beginScope();
		for (Token param :function.params) {
			declare(param);
			define(param);
		}
		resolve(function.body);
		endScope();
	}

	@Override
	public Void visitIfStmt(If stmt) {
		resolve(stmt.condition);
		resolve(stmt.thenBranch);
		if (stmt.elseBranch!=null) resolve(stmt.elseBranch);
		return null;
	}

	@Override
	public Void visitPrintStmt(Print stmt) {
		resolve(stmt.expression);
		return null;
	}

	@Override
	public Void visitReturnStmt(Return stmt) {
		if (stmt.value != null) {
			resolve(stmt.value);
		}
		
		return null;
	}

	@Override
	public Void visitVarStmt(Var stmt) {
		declare(stmt.name);
		if(stmt.initializer != null) {
			resolve(stmt.initializer);
		}
		define(stmt.name);
		return null;
	}

	@Override
	public Void visitWhileStmt(While stmt) {
		resolve(stmt.condition);
		resolve(stmt.body);
		return null;
	}

	@Override
	public Void visitBreakStmt(Break stmt) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Void visitAssignExpr(Assign expr) {
		resolve(expr.value);
		resolveLocal(expr, expr.name);
		return null;
	}

	@Override
	public Void visitBinaryExpr(Binary expr) {
		resolve(expr.left);
		resolve(expr.right);
		return null;
	}

	@Override
	public Void visitCallExpr(Call expr) {
		resolve(expr.callee);
		
		for (Expr argument : expr.arguments) {
			resolve(argument);
		}
		
		return null;
	}

	@Override
	public Void visitGroupingExpr(Grouping expr) {
		resolve(expr.expression);
		return null;
	}

	@Override
	public Void visitLiteralExpr(Literal expr) {
		return null;
	}

	@Override
	public Void visitLogicalExpr(Logical expr) {
		resolve(expr.left);
		resolve(expr.right);
		return null;
	}

	@Override
	public Void visitUnaryExpr(Unary expr) {
		resolve(expr.right);
		return null;
	}

	@Override
	public Void visitVariableExpr(Variable expr) {
		if (!scopes.isEmpty() &&
				scopes.peek().get(expr.name.lexeme)==Boolean.FALSE) {
			Lox.error(expr.name, 
					"Can't read local variable in"
					+ " its own initilaizer.");
		}
		
		resolveLocal(expr,expr.name);
		return null;
	}

	private void resolveLocal(Expr expr, Token name) {
		for (int i = scopes.size() - 1; i>=0;i--) {
			if (scopes.get(i).containsKey(name.lexeme)) {
				interpreter.resolve(expr, scopes.size() -1 - i);
				return;
			}
		}
	}

}
