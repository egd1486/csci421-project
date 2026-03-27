package Common;
import BufferManager.BufferManager;
import Catalog.*;
import static Common.TokenType.*;
import Common.WhereTree.*;
import java.util.*;


public class Parser {

    private static final Set<TokenType> PossibleOps = Set.of(
            EQUAL, NOT_EQUAL, LESS, GREATER, LESS_EQUAL, GREATER_EQUAL,
            IS
    );
    private static final Set<TokenType> PossibleVals = Set.of(
            NAME_LITERAL, INT_LITERAL, DOUBLE_LITERAL, STRING_LITERAL,
            TRUE, FALSE, NULL
    );

    private static final Set<TokenType> PossibleMath = Set.of(
            PLUS, MINUS, MULT, DIV
    );

    private static final Set<TokenType> Literals = Set.of(
            INT_LITERAL, DOUBLE_LITERAL, STRING_LITERAL, TRUE, FALSE, NULL
    );

    private static int Create(int Index, Token[] Input) throws Exception {
        // Validate syntax for "TABLE <name>"
        Token T; 
        Validate(Input[Index], TABLE);

        // Grab and validate name,
        Validate(T = Input[++Index], NAME_LITERAL);
        String Name = T.Literal;

        // Validate syntax for parenthesis
        Validate(Input[++Index], LPAREN);

        // Validate exactly 1 primary key, keep a count.
        int Primary = 0;

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
                case NOTNULL -> A.notNull = true;
                case UNIQUE -> A.unique = true;
                case PRIMARYKEY -> {
                    if (Primary++ > 0) throw new Exception("Table cannot have more than 1 primary key");
                    else A.primaryKey = true;
                }
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
        S.AddAttribute(A.name, A.type, A.typeLength, A.notNull, A.primaryKey, A.unique, A.defaultVal, false);

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
            // Check if NAME_LITERAL DOT NAME_LITERAL (Table.Column)
            if (Input[Index].Type == PERIOD) {
                String cartesianCol = T.Literal;
                Index++; // consume PERIOD
                cartesianCol = cartesianCol + ".";
                Validate(Input[Index], NAME_LITERAL);  //consume column name
                cartesianCol = cartesianCol + Input[Index].Literal;
                Columns.add(cartesianCol);
                Index++;
            } else {
                Columns.add(T.Literal);
                // Index++;
            }

            while ((T = Input[Index]).Type == COMMA) {
                T=Input[++Index];
                Validate(T, NAME_LITERAL);
                if (Input[Index + 1].Type == PERIOD) { //look ahead if period
                    String cartesianCol = T.Literal;
                    Index++; //consume period
                    Index++; //consume column name
                    Validate(Input[Index], NAME_LITERAL);
                    cartesianCol = cartesianCol + "." + Input[Index].Literal;
                    Columns.add(cartesianCol);
                } else {
                    Columns.add(T.Literal);
                }
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

        // Check if all schemas exist
        // and if the column names are valid, and not ambiguous.
        Map<String, Integer> NameCount = new HashMap<>();
        for(String tableName : Tables){
            Schema S = Catalog.GetSchema(tableName);

            if (S == null) throw new Exception("Table " + tableName + " does not exist.");

            for (Attribute A : S.Attributes) {
                if (NameCount.containsKey(A.name)) 
                NameCount.put(A.name, NameCount.get(A.name) + 1);
                else NameCount.put(A.name, 1);
                // Handle checking for qualified column names
                NameCount.put(tableName+"."+A.name, 1);
            }
        }

        // Throw exception of ambiguous column name if found in more than one table
        for (String C : Columns)            
        if (NameCount.containsKey(C)) {
            if (NameCount.get(C) > 1)
            throw new Exception("Column " + C + " is ambiguous.");
        } else throw new Exception("Column " + C + " does not exist.");
        
        WhereClassInterface WhereTree = null;
        //Check the next Token for Where or Orderby
        if(Input[Index].Type == WHERE){
            WhereResult WhereRS = Where(++Index, Input, Tables);
            WhereTree = WhereRS.WhereNode;
            System.out.println("Where Tree: " + WhereTree.print());
            Index = WhereRS.Index;
        }

        // If we got here, great. Check for semicolon and complete the select.
        Validate(Input[Index], SEMICOLON);

        if (All && Tables.size() == 1) {
            Schema S = Catalog.GetSchema(Tables.get(0));

            if (S == null) throw new Exception("Table " + Tables.get(0) + " does not exist.");

            S.DisplayTable(WhereTree, new ArrayList<>());
        } else if (All && Tables.size() >= 2) {
            Schema combindSchema = Catalog.GetSchema(Tables.get(0));
            if (combindSchema == null) throw new Exception("Table " + Tables.get(0) + " does not exist.");
            for(int idx = 1; idx < Tables.size(); idx++) {
                Schema sx = Catalog.GetSchema(Tables.get(idx));
                if (sx == null) throw new Exception("Table " + Tables.get(idx) + " does not exist.");
                combindSchema = combindSchema.cartesianJoin(combindSchema, sx);
            }
            combindSchema.DisplayTable(WhereTree, new ArrayList<>());
        } else if (!All && Tables.size() == 1) { //single table
            Schema S = Catalog.GetSchema(Tables.get(0));
            if (S == null) throw new Exception("Table " + Tables.get(0) + " does not exist.");
            // keep only values in requested columns
            S.DisplayTable(WhereTree, Columns);
        } else if (!All && Tables.size() >= 2) { //multiple tables
            Schema combindSchema = Catalog.GetSchema(Tables.get(0));
            if (combindSchema == null) throw new Exception("Table " + Tables.get(0) + " does not exist.");
            for(int idx = 1; idx < Tables.size(); idx++) {
                Schema sx = Catalog.GetSchema(Tables.get(idx));
                if (sx == null) throw new Exception("Table " + Tables.get(idx) + " does not exist.");
                combindSchema = combindSchema.cartesianJoin(combindSchema, sx);
            }
            combindSchema.DisplayTable(WhereTree, Columns);
        }

        return ++Index;
    }

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

        // FROM
        Validate(Input[Index++], FROM);
        ArrayList<String> Tables = new ArrayList<>();

        // <table>
        Token T = Input[Index++];
        Validate(T, NAME_LITERAL);
        Tables.add(T.Literal);
        String Name = T.Literal;

        Schema oldSchema = Catalog.GetSchema(Name), newSchema;
        if (oldSchema == null)
        throw new Exception("Table " + Name + " does not exist.");

        // WHERE <condition>
        WhereResult WhereRS = null;
        if(Input[Index].Type == WHERE){
            WhereRS = Where(++Index, Input, Tables);
            Index = WhereRS.Index;
        }

        // Semicolon
        Validate(Input[Index], SEMICOLON);
        // Make new table using schema of old table

        newSchema = oldSchema.Copy(WhereRS, null, null);
        // Drop old schema,
        Catalog.RemoveSchema(Name);
        // Swap with the new one.
        Catalog.Schemas.add(newSchema);
 
        return ++Index;
    }

    // make copy of OG table schema
    // make new table, loop through everything in original table, if it makes the where tree false add to table
    // if make true dont add
    // drop OG table and rename new table
    private static int Update(int Index, Token[] Input) throws Exception {

        //consume table name
        Validate(Input[Index], NAME_LITERAL);
        ArrayList<String> Tables = new ArrayList<>();
        Tables.add(Input[Index].Literal);
        String tableName = Input[Index].Literal;
        Schema oldSchema = Catalog.GetSchema(tableName);

        if (oldSchema == null) 
        throw new Exception("Table " + tableName + " does not exist.");

        Validate(Input[++Index], SET);
        ++Index;

        Token Column = Input[Index++];
        Token Equal = Input[Index++];
        Token Value = Input[Index++];

        //Validation
        Validate(Column, EQUAL);
        Validate(Equal, EQUAL);
        if(!(PossibleVals.contains(Value.Type))){
            throw new Exception("Unexpected token " + Value.Type.toString() + ", expected PossibleVal type. In Update Function");
        }

        InterfaceOperandNode valueNode = null;
        if(Literals.contains(Value.Type)){
            valueNode = new ConstantValueNode(Value.Literal,Value.Type);
        }else{
            //Have to be Name-Literal
            valueNode = new AttributeValueNode(tableName,Value.Literal,Tables);
        }

        //Look Ahead to check for mathematical expressions
        if(PossibleMath.contains(Input[Index].Type)){
            Token MathematicalOperator = Input[Index++];
            Token Lookahead = Input[Index++];
            //Check if next Value is Literal or Attribute
            if(!(PossibleVals.contains(Lookahead.Type))){
                throw new Exception("Unexpected token " + Value.Type.toString() + ", expected PossibleVal type. In Update Function");
            }
            if(Literals.contains(Lookahead.Type)){
                valueNode = new ArithmeticOpNode(valueNode, MathematicalOperator.Type, new ConstantValueNode(Lookahead.Literal, Lookahead.Type));
            }else{
                valueNode = new ArithmeticOpNode(valueNode, MathematicalOperator.Type, new AttributeValueNode(tableName,Lookahead.Literal,Tables));
            }
        }

        //Need Where on SET (If provided)
        WhereResult WhereRS = null;
        if (Input[Index].Type == WHERE) {
            WhereRS = Where(++Index, Input, Tables);
            Index  = WhereRS.Index;
        }
        Validate(Input[Index], SEMICOLON);

        Schema newSchema = oldSchema.Copy(WhereRS, (WhereClassInterface) valueNode, Column.Literal);
        // Remove old table and pages,
        Catalog.RemoveSchema(tableName);
        // Swap it with our new copy.
        Catalog.Schemas.add(newSchema);

        return ++Index;
    }

    private static void Validate (Token Given, TokenType Expected) throws Exception {
        if (Given.Type != Expected)
        throw new Exception("Unexpected token " + Given.Type.toString() + ", expected " + Expected.toString());
    }

    // Compares the priority of the first token with the second token
    // Returns true if second token has higher priority than first
    private static boolean compareOperators(Token first, Token second){
        if(second == null) return false;
        else if(first.Type == OR) return true;
        else if(first.Type == AND && PossibleOps.contains(second.Type)) return true;
        else if(PossibleOps.contains(first.Type)) return false;
        else return false;
    }

    /**
     * Makes a Where Tree
     * @param Index Current Index
     * @param Input Token List
     * @param table What tables were working with
     * @return WhereResult an Object that returns (WhereTree and Index)
     * @throws Exception
     */
    private static WhereResult Where(int Index, Token[] Input, ArrayList<String> table) throws Exception{
        Deque<InterfaceOperandNode> vals = new ArrayDeque<>(); // For Values
        Deque<Token> ops = new ArrayDeque<>(); // For Operators
        Deque<WhereClassInterface> whereTreeNodes = new ArrayDeque<>(); // WhereTree
        boolean simplifyMathOperation = false; // If we need to reduce a math expression

        // Loop Through TokenList until we hit either SEMICOLON or ORDERBY
        while(Input[Index].Type != SEMICOLON && Input[Index].Type != ORDERBY){

            Token T = Input[Index++]; //Current Index | Index get added

            //If Token is one of the mathematical operation signal to reduce math expression
            if(PossibleMath.contains(T.Type)){
                ops.push(T);
                if(simplifyMathOperation){
                    throw new Exception("Repeated Mathematical Expression Detected"); //Detects if we have + + or - - by accident
                }
                simplifyMathOperation = true;
            }

            // Handling if token is AND/OR
            else if(T.Type == AND || T.Type == OR){
                // while there are operators with higher priority on top of op stack
                while(compareOperators(T, ops.peek())){
                    Token op = ops.pop();
                    if(PossibleOps.contains(op.Type)){
                        InterfaceOperandNode right = vals.pop();
                        InterfaceOperandNode left = vals.pop();
                        whereTreeNodes.push(new BinaryOpNode(left, op.Type, right));
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

                    if(PossibleOps.contains(next.Type)){
                        int sameAttrName = 0;
                        String name = null;
                        for(String tableName : table){
                            if(Schema.getAttribute(T.Literal, Catalog.GetSchema(tableName)) != null){
                                sameAttrName++;
                                name = tableName;
                            }
                            if(sameAttrName > 1) throw new Exception("Ambiguous, unqualified attribute name: " + T.Literal + ". Must specify table.");
                        }
                        if(sameAttrName == 0) throw new Exception("Attribute " + T.Literal + " does not exist in any table");
                        vals.push(new AttributeValueNode(name, T.Literal, table));
                    }

                    //Else were working with multiple tables
                    else if(next.Type == PERIOD){

                        //Check if the table is valid
                        if(!(table.contains(T.Literal))){
                            throw new Exception("Table: " + T.Literal + " is not a valid table. Provided Tables: " + table);
                        }

                        Index++; // Move on from period
                        Token attrName = Input[Index++];

                        //Check if the attribute exists in the table
                        if(Schema.getAttribute(attrName.Literal, Catalog.GetSchema(T.Literal)) == null){
                            throw new Exception("Attribute " + attrName.Literal + " does not exist in table: " + T.Literal);
                        }
                        if(attrName.Type != NAME_LITERAL){
                            throw new Exception("Unexpected tokens: " + T.Type + ", " + T.Type + ", " + attrName.Type.toString() + " | Expected tokens: NAME_LITERAL, PERIOD, NAME_LITERAL");
                        }

                        //Create AttributeValueNode given Schema and Column and push it into vals
                        AttributeValueNode attributeval = new AttributeValueNode(T.Literal, attrName.Literal, table);

                        vals.push(attributeval);

                    }
                    else throw new Exception("Unexpected tokens: " + T.Type + ", " + next.Type.toString() + " | Expected tokens: NAME_LITERAL, PERIOD or Operator");
                }
                else if(PossibleVals.contains(T.Type)) vals.push(new ConstantValueNode(T.Literal, T.Type)); //Working with Constant Values
                else throw new Exception("Unexpected token: " + T.Type.toString() + " expected literal value");

                //If there's a mathematical operation we reduce the expression first and turn it into ArithemeticOpNode
                if(simplifyMathOperation){
                    InterfaceOperandNode Right  = vals.pop();
                    InterfaceOperandNode Left = vals.pop();
                    Token MathOperation = ops.pop();
                    vals.push(new ArithmeticOpNode(Left, MathOperation.Type, Right));
                    simplifyMathOperation = false; //We finish working with mathematical operation
                }
            }

            // Handling if token is IS/IS NOT
            else if(T.Type == IS){
                Token next = Input[Index];
                if(next.Type == NOT){
                    ops.push(next);
                    Index++;
                }
                else ops.push(T);
            }
            // Handling if token is a relational operator
            else ops.push(T);
        }

        // handling leftover operators
        while(!ops.isEmpty()){
            Token op = ops.pop();
            switch(op.Type){
                case EQUAL, NOT_EQUAL, LESS, GREATER, LESS_EQUAL, GREATER_EQUAL, IS, NOT -> {
                    InterfaceOperandNode right = vals.pop();
                    InterfaceOperandNode left = vals.pop();
                    whereTreeNodes.push(new BinaryOpNode(left, op.Type, right));
                }
                case AND, OR -> {
                    WhereClassInterface right = whereTreeNodes.pop();
                    WhereClassInterface left = whereTreeNodes.pop();
                    whereTreeNodes.push(op.Type == AND ? new AndNode(left, right) : new OrNode(left, right));
                }
                default -> {
                    throw new Exception("Unexpected token: " + op.Type.toString() + " expected literal value");
                }
            }
        }
        // All nodes should be a part of one main node at this point
        if(whereTreeNodes.size() != 1)
        throw new Exception("Error in parsing Where Tree, final size should be 1");
        return new WhereResult(whereTreeNodes.pop(), Index);
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
