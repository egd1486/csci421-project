package Common;
import Catalog.*;
import static Common.TokenType.*;

import java.sql.SQLOutput;
import java.util.*;

import Common.WhereTree.*;

import javax.print.DocFlavor;

public class Parser {

    private static int Create(int Index, Token[] Input) throws Exception {
        // Validate syntax for "TABLE <name>"
        Token T; 
        Validate(Input[Index], TABLE);

        // Grab and validate name,
        Validate(T = Input[++Index], NAME_LITERAL);
        String Name = T.Literal;

        // Validate syntax for parenthesis
        Validate(Input[++Index], LPAREN);

        // Validate exactly 1 primary key
        int primary = 0;
        for (Token token : Input){
            if (token.Literal.equals("PRIMARYKEY"))
                primary ++;
        }
        if (primary != 1){
            throw new Exception("Table must contain exactly one PRIMARYKEY");
        }

        // Begin parsing attributes and their properties.
        ArrayList<Attribute> Attributes = new ArrayList<>();

        while (true) {
            // Get name
            Validate(T = Input[++Index], NAME_LITERAL);
            String AttributeName = T.Literal;

            // Get type
            T = Input[++Index];
            Type AType = null;
            int Length = 0;
            switch (T.Type) {
                case BOOLEAN -> AType = Type.BOOLEAN;
                case DOUBLE -> AType = Type.DOUBLE;
                case INTEGER -> AType = Type.INT;
                // Char types have a size provided, so must read that.
                case CHAR, VARCHAR -> {
                    AType = T.Type == CHAR ? Type.CHAR : Type.VARCHAR;

                    Validate(Input[++Index], LPAREN); // Paren 1
                    Validate(T = Input[++Index], INT_LITERAL); // Integer size,
                    Length = Integer.parseInt(T.Literal);
                    Validate(Input[++Index], RPAREN); // Paren 2
                }

                default -> throw new Exception("Unexpected token " + T.Type.toString() + ", expected column's type.");
            }

            // Create attribute object,
            Attribute A = new Attribute(AttributeName, AType, Length, false, false, false, null);

            // Now read tokens for qualifiers until comma or parenthesis is closed.
            while ((T = Input[++Index]).Type != COMMA && T.Type != RPAREN)
            switch (T.Type) {
                case PRIMARYKEY -> A.primaryKey = true;
                case NOTNULL -> A.notNull = true;
                case UNIQUE -> A.unique = true;
                case DEFAULT -> {
                    // Grab the provided default value..?
                    T = Input[++Index];
                    if (T.Type!=STRING_LITERAL && T.Type!=INT_LITERAL && T.Type!=DOUBLE_LITERAL && T.Type!=TRUE && T.Type!=FALSE) 
                    throw new Exception("Unexpected token " + T.Type.toString() + ", expected default value.");
                    A.defaultVal = A.Parse(T.Literal);
                }
                default -> throw new Exception("Unexpected token " + T.Type.toString() + ", expected attribute qualifier.");
            }    

            Attributes.add(A);

            if (T.Type == RPAREN) break; // Break once reached end of attributes (right paren)
        }

        // Now that we have all the attributes and have readched the rparen, check for semicolon
        Validate(Input[++Index], SEMICOLON);
        // If we got here, great. Create the table.
        Schema S = Catalog.AddSchema(Name);

        // Loop through and call through AddAttribute for validation,
        for (Attribute A : Attributes) 
        S.AddAttribute(A.name, A.type, A.typeLength, A.notNull, A.primaryKey, A.unique, A.defaultVal);

        // Return start of next command, which is past semicolon.
        return ++Index;
    }

    private static int Select(int Index, Token[] Input) throws Exception {
        boolean All = false;
        ArrayList<String> Columns = new ArrayList<>();
        ArrayList<String> Tables = new ArrayList<>();
        Token T = Input[Index++];

        // If star, do a normal display.
        if (T.Type == STAR) All = true;
        // Otherwise, read column names until we hit what's SUPPOSED to be from.
        else if (T.Type == NAME_LITERAL) {
            Columns.add(T.Literal);

            while ((T = Input[Index]).Type == COMMA) {
                Validate(T=Input[++Index], NAME_LITERAL);
                Columns.add(T.Literal);
                Index++;
            }
        } 
        // Other token types are not valid here.
        else throw new Exception("Unexpected token " + T.Type.toString() + ", expected * or column name(s).");

        // Now we get the tables, validate from first.
        Validate(Input[Index++], FROM);

        // At least one table,
        T = Input[Index++];
        Validate(T, NAME_LITERAL);
        Tables.add(T.Literal);

        // Parse more if available
        while (Input[Index].Type == COMMA) {
            T = Input[++Index];
            Validate(T, NAME_LITERAL);
            Tables.add(T.Literal);
            Index++;
        }


        // If we got here, great. Check for semicolon and complete the select.
        Validate(Input[Index], SEMICOLON);

        if (All && Tables.size() == 1) {
            Schema S = Catalog.GetSchema(Tables.get(0));

            if (S == null) throw new Exception("Table " + Tables.get(0) + " does not exist.");

            S.DisplayTable();
        }

        return ++Index;
    }

    private static TokenType[] Literals = {INT_LITERAL, DOUBLE_LITERAL, STRING_LITERAL, TRUE, FALSE, NULL};
    private static int Insert(int Index, Token[] Input) throws Exception {
        // Get table's name,
        Token T = Input[Index]; 
        Validate(T, NAME_LITERAL);
        String TableName = T.Literal;

        // Check for key word VALUES,
        Validate(Input[++Index], VALUES);

        // Check for left paren,
        Validate(Input[++Index], LPAREN);

        // Now we can parse rows.
        ArrayList<ArrayList<Object>> Rows = new ArrayList<>();

        // Grab schema for attribute length and type validation.
        Schema S = Catalog.GetSchema(TableName);

        if (S == null) throw new Exception("Table " + TableName + " does not exist.");

        //Shift onto literal values,
        Index++;

        while (true) {
            ArrayList<Object> Row = new ArrayList<>();

            while ((T = Input[Index]).Type != RPAREN && T.Type != COMMA) {
                // Check if any literal type
                boolean Literal = false;
                for (TokenType L : Literals) Literal |= T.Type == L;
                if (!Literal) throw new Exception("Unexpected token " + T.Type.toString() + ", expected literal value.");
                
                // Otherwise, add literal
                Row.add(T.Literal);
                Index++;
            }

            if (Row.size() > 0) Rows.add(Row);

            if (T.Type == RPAREN) break;
            else Index++;
        }

        // Check for semicolon,
        Validate(Input[++Index], SEMICOLON);

        // Great! now we have all the rows to insert,
        int count = 0;
        try {
            for (ArrayList<Object> Row : Rows) {
                // Iterate through each column and parse into actual data type,
                for (int i = 0; i < S.Attributes.size(); i++) 
                Row.set(i, S.Attributes.get(i).Parse(Row.get(i)));
                
                // Then run insert.
                S.Insert(Row);
                count++;
            }
        } catch (Exception e) {
            System.out.println("Inserted " + count + " rows.");
            throw e;
        } 
        System.out.println("Inserted " + count + " rows.");

        return ++Index;
    }

    private static int Alter(int Index, Token[] Input) throws Exception {
        // Validate syntax for "TABLE <name>"
        Token T; 
        Validate(Input[Index], TABLE);

        // Get actual name,
        Validate(T = Input[++Index], NAME_LITERAL);
        String Name = T.Literal;

        // Check for alter type,
        T = Input[++Index];
        boolean Add = T.Type == ADD;

        // Throw if not add or drop,
        if (!Add && T.Type != DROP) 
        throw new Exception("Unexpected token " + T.Type.toString() + ", expected ADD or DROP.");

        // Grab attribute name,
        Validate(T = Input[++Index], NAME_LITERAL);
        String AttributeName = T.Literal;

        if (!Add) {
            // Check for semicolon,
            Validate(Input[++Index], SEMICOLON);

            // Drop the attribute
            Catalog.AttributeDrop(Name, AttributeName);
        }
        // Otherwise, this is add, and things get complicated..
        else {
            // Get type
            T = Input[++Index];
            Type AType = null;
            int Length = 0;
            switch (T.Type) {
                case BOOLEAN -> AType = Type.BOOLEAN;
                case DOUBLE -> AType = Type.DOUBLE;
                case INTEGER -> AType = Type.INT;
                // Char types have a size provided, so must read that.
                case CHAR, VARCHAR -> {
                    AType = T.Type == CHAR ? Type.CHAR : Type.VARCHAR;

                    Validate(Input[++Index], LPAREN); // Paren 1
                    Validate(T = Input[++Index], INT_LITERAL); // Integer size,
                    Length = Integer.parseInt(T.Literal);
                    Validate(Input[++Index], RPAREN); // Paren 2
                }

                default -> throw new Exception("Unexpected token " + T.Type.toString() + ", expected column's type.");
            }

            // Create attribute object,
            Attribute A = new Attribute(AttributeName, AType, Length, false, false, false, null);

            // Now read tokens for qualifiers until semicolon, or until it runs out of the domain and blows up (which is fine).
            while ((T = Input[++Index]).Type != SEMICOLON)
            switch (T.Type) {
                case PRIMARYKEY -> A.primaryKey = true;
                case NOTNULL -> A.notNull = true;
                case UNIQUE -> A.unique = true;
                case DEFAULT -> {
                    // Grab the provided default value..?
                    T = Input[++Index];
                    if (T.Type!=STRING_LITERAL && T.Type!=INT_LITERAL && T.Type!=DOUBLE_LITERAL && T.Type!=TRUE && T.Type!=FALSE) 
                    throw new Exception("Unexpected token " + T.Type.toString() + ", expected default value.");
                    A.defaultVal = A.Parse(T.Literal);
                }
                default -> throw new Exception("Unexpected token " + T.Type.toString() + ", expected attribute qualifier.");
            }

            Catalog.AttributeAdd(Name, A.name, A.type, A.typeLength, A.primaryKey, A.notNull, A.unique, A.defaultVal);
        }

        return ++Index;
    }

    private static int Drop(int Index, Token[] Input) throws Exception {
        // Validate syntax for "TABLE <name>"
        Token T; 
        Validate(Input[Index], TABLE);

        // Get actual name,
        Validate(T = Input[++Index], NAME_LITERAL);
        String Name = T.Literal;

        // Check for semi colon,
        Validate(Input[++Index], SEMICOLON);

        // Drop the table.
        Catalog.RemoveSchema(Name);

        System.out.println("Table: " + Name + " dropped successfully");

        return ++Index;
    }

    private static int Delete(int Index, Token[] Input) throws Exception { 
        // TODO

        return Index; 
    }

    private static int Update(int Index, Token[] Input) throws Exception { 
        // TODO
        
        return Index; 
    }

    private static void Validate (Token Given, TokenType Expected) throws Exception {
        if (Given.Type != Expected)
        throw new Exception("Unexpected token " + Given.Type.toString() + ", expected " + Expected.toString());
    }

    private static final Set<TokenType> PossibleOps = Set.of(
            EQUAL, NOT_EQUAL, LESS, GREATER, LESS_EQUAL, GREATER_EQUAL,
            PLUS, MINUS, MULT, DIV, IS
    );
    private static final Set<TokenType> PossibleVals = Set.of(
            NAME_LITERAL, INT_LITERAL, DOUBLE_LITERAL, STRING_LITERAL,
            TRUE, FALSE, NULL
    );

    // Compares the priority of the first token with the second token
    // Returns true if second token has higher priority than first
    private static boolean compareOperators(Token first, Token second){
        if(second == null) return false;
        else if(first.Type == OR) return true;
        else if(first.Type == AND && PossibleOps.contains(second.Type)) return true;
        else if(PossibleOps.contains(first.Type)) return false;
        else return false;
    }

    private static BinaryOpNode createBinaryOpNode(Token leftToken, Token op, Token rightToken) throws Exception{
        Token[] tokens = {leftToken, rightToken};
        ArrayList<InterfaceOperandNode> IOPs = new ArrayList<>();
        for(Token token : tokens){
            // TODO: How to create AttributeValueNode? Have column name, but not schema
            // Is plan to pass table names into Where and then pass here?
            if(token.Type == NAME_LITERAL) IOPs.add(new AttributeValueNode(___, token.Literal));
            else{
                // ConstantValueNode requires Type, so parsing TokenType for Type
                Type type;
                switch (token.Type){
                    case INT_LITERAL -> type = Type.INT;
                    case DOUBLE_LITERAL -> type = Type.DOUBLE;
                    case TRUE, FALSE -> type = Type.BOOLEAN;
                    case NULL -> type = Type.NULL;
                    // TODO: How to deal with STRING_LITERAL?
                    // Need to find varchar/char, need schema
                    default -> throw new Exception("Expected literal value but got value of type " + token.Type);
                }
                IOPs.add(new ConstantValueNode(token.Literal, type));
            }
        }
        return new BinaryOpNode(IOPs.getFirst(), op.Type, IOPs.getLast());
    }

    private static WhereClassInterface Where(int Index, Token[] Input) throws Exception{
        Deque<Token> vals = new ArrayDeque<>();
        Deque<Token> ops = new ArrayDeque<>();
        Deque<WhereClassInterface> whereTreeNodes = new ArrayDeque<>();
        while(Input[Index].Type != SEMICOLON){
            Token T = Input[Index++];
            // Handling if token is AND/OR
            if(T.Type == AND || T.Type == OR){
                // while there are operators with higher priority on top of op stack
                while(compareOperators(T, ops.peek())){
                    Token op = ops.pop();
                    if(PossibleOps.contains(op.Type)){
                        Token right = vals.pop();
                        Token left = vals.pop();
                        whereTreeNodes.push(createBinaryOpNode(left, op, right));
                        continue;
                    }
                    else if(op.Type != AND && op.Type != OR){
                        throw new Exception("Unexpected token: " + op.Type.toString() + ", expected operator or AND/OR");
                    }
                    WhereClassInterface right = whereTreeNodes.pop();
                    WhereClassInterface left = whereTreeNodes.pop();
                    whereTreeNodes.push(op.Type == AND ? new AndNode(left, right) : new OrNode(left, right));
                }
                ops.push(T);
            }
            // Handling if token is a value
            else if(!PossibleOps.contains(T.Type)){
                if(T.Type == NAME_LITERAL){
                    Token next = Input[Index];
                    if(PossibleOps.contains(next.Type)) vals.push(T);
                    else if(next.Type == PERIOD){
                        Index++;
                        Token attrName = Input[Index++];
                        if(attrName.Type != NAME_LITERAL){
                            throw new Exception("Unexpected tokens: " + T.Type + ", " + next.Type + ", " + attrName.Type.toString() + " | Expected tokens: NAME_LITERAL, PERIOD, NAME_LITERAL");
                        }
                        // TODO: Need to push attrName onto stack, but must retain table it belongs to

                    }
                    else throw new Exception("Unexpected tokens: " + T.Type + ", " + next.Type.toString() + " | Expected tokens: NAME_LITERAL, PERIOD or Operator");
                }
                else if(PossibleVals.contains(T.Type)) vals.push(T);
                else throw new Exception("Unexpected token: " + T.Type.toString() + ", expected literal value");
            }
            // Handling if token is a relational operator
            else ops.push(T);
        }
        // handling leftover operators
        while(!ops.isEmpty()){
            Token op = ops.pop();
            if(PossibleOps.contains(op.Type)){
                Token right = vals.pop();
                Token left = vals.pop();
                whereTreeNodes.push(createBinaryOpNode(left, op, right));
            }
            else if(op.Type == AND || op.Type == OR){
                WhereClassInterface right = whereTreeNodes.pop();
                WhereClassInterface left = whereTreeNodes.pop();
                whereTreeNodes.push(op.Type == AND ? new AndNode(left, right) : new OrNode(left, right));
            }
            else{
                throw new Exception("Unexpected token: " + op.Type + ", expected operator");
            }
        }
        // All nodes should be a part of one main node at this point
        if(whereTreeNodes.size() != 1)
        throw new Exception("Error in parsing Where Tree, final size should be 1");
        return whereTreeNodes.pop();
    }

    // NOTE: Any parse functions must return the index position AFTER their semicolon.
    // Parse functions also expect to be given the Index of the SECOND token they need to parse
    // E.g. INSERT tablename expects to start on tablename.
    public static void parse(Token[] Input) throws Exception {
        int Index = 0;
        // for (Token T : Input) System.out.println(T.Type.toString());
        try {
            while (Index < Input.length)
            switch (Input[Index++].Type) {
                // Key word and function associations here!!!
                // Phase 1
                case CREATE ->  Index = Create(Index, Input);
                case SELECT ->  Index = Select(Index, Input);
                case INSERT ->  Index = Insert(Index, Input);
                case ALTER  ->  Index = Alter(Index, Input);
                case DROP   ->  Index = Drop(Index, Input);
                // Phase 2
                case DELETE ->  Index = Delete(Index, Input);
                case UPDATE ->  Index = Update(Index, Input);
                default     ->  throw new Exception("Unexpected token " + Input[Index-1].Type.toString());
            }
        } catch (Exception e) {
            if (e instanceof ArrayIndexOutOfBoundsException)
            throw new Exception("Unexpected end of input");
            else throw e;
        }
    }
}
