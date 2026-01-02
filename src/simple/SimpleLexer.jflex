package simple;
import java_cup.runtime.Symbol;

%%

// =====================================================================
// SECTION 1: JFlex Options & Imports
// =====================================================================

%class SimpleLexer
%public
%cup
%line
%column

// =====================================================================
// SECTION 2: Lexer States & Macros
// =====================================================================

IntegerLiteral = [0-9]+
Identifier = [a-zA-Z][a-zA-Z0-9_]*

%%

// =====================================================================
// SECTION 3: Token Recognition Rules
// =====================================================================

<YYINITIAL> {
    // --- Whitespace (ignored) ---
    [ \t\r\n]+          { /* Just skip */ }

    // --- Punctuation ---
    "("               { return new Symbol(sym.LEFT_PAREN, yyline, yycolumn); }
    ")"               { return new Symbol(sym.RIGHT_PAREN, yyline, yycolumn); }
    "{"               { return new Symbol(sym.LEFT_BRACE, yyline, yycolumn); }
    "}"               { return new Symbol(sym.RIGHT_BRACE, yyline, yycolumn); }
    ","               { return new Symbol(sym.COMMA, yyline, yycolumn); }
    ";"               { return new Symbol(sym.SEMICOLON, yyline, yycolumn); }

    // --- Operators ---
    ":="              { return new Symbol(sym.ASSIGN, yyline, yycolumn); }
    "="               { return new Symbol(sym.EQ, yyline, yycolumn); }
    "<"               { return new Symbol(sym.LT, yyline, yycolumn); }
    ">"               { return new Symbol(sym.GT, yyline, yycolumn); }
    "+"               { return new Symbol(sym.PLUS, yyline, yycolumn); }
    "-"               { return new Symbol(sym.MINUS, yyline, yycolumn); }
    "*"               { return new Symbol(sym.STAR, yyline, yycolumn); }
    "/"               { return new Symbol(sym.SLASH, yyline, yycolumn); }

    // --- Keywords ---
    "func"            { return new Symbol(sym.FUNC, yyline, yycolumn); } // ADDED
    "if"              { return new Symbol(sym.IF, yyline, yycolumn); }
    "else"            { return new Symbol(sym.ELSE, yyline, yycolumn); }
    "while"           { return new Symbol(sym.WHILE, yyline, yycolumn); }
    "return"          { return new Symbol(sym.RETURN, yyline, yycolumn); }
    "var"             { return new Symbol(sym.VAR, yyline, yycolumn); } // ADDED
    "int"             { return new Symbol(sym.INT, yyline, yycolumn); }
    "boolean"         { return new Symbol(sym.BOOLEAN, yyline, yycolumn); }
    "true"            { return new Symbol(sym.TRUE, yyline, yycolumn); }
    "false"           { return new Symbol(sym.FALSE, yyline, yycolumn); }

    // --- Literals and Identifiers (must be last) ---
    {IntegerLiteral}  { return new Symbol(sym.INTEGER_LIT, yyline, yycolumn, Integer.parseInt(yytext())); }
    {Identifier}      { return new Symbol(sym.IDENTIFIER, yyline, yycolumn, yytext()); }
}

// Catches any other character and throws an error
[^] { throw new RuntimeException("Illegal character '" + yytext() + "' at line " + yyline + ", column " + yycolumn); }

