package Common;
import Catalog.*;
import static Common.TokenType.*;
import Common.WhereTree.*;
import java.util.*;


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

        WhereClassInterface WhereTree = null;
        //Check the next Token for Where or Orderby
        if(Input[Index].Type == WHERE){
            WhereResult WhereRS = Where(++Index, Input, Tables);
            WhereTree = WhereRS.WhereNode;
            System.out.println(WhereTree.print());
            Index = WhereRS.Index;
        }

        // If we got here, great. Check for semicolon and complete the select.
        Validate(Input[Index], SEMICOLON);

        if (All && Tables.size() == 1) { //single table
            Schema S = Catalog.GetSchema(Tables.get(0));

            if (S == null) throw new Exception("Table " + Tables.get(0) + " does not exist.");

            S.DisplayTable(WhereTree);
        } else if (All && Tables.size() >= 2) { //multiple tables
            Schema combindSchema = Catalog.GetSchema(Tables.get(0));
            if (combindSchema == null) throw new Exception("Table " + Tables.get(0) + " does not exist.");
            for(int idx = 1; idx < Tables.size(); idx++) {
                Schema sx = Catalog.GetSchema(Tables.get(idx));
                if (sx == null) throw new Exception("Table " + Tables.get(idx) + " does not exist.");
                combindSchema = combindSchema.cartesianJoin(combindSchema, sx);
            }
            combindSchema.DisplayTable(WhereTree);
        } else if (!All && Tables.size() == 1) { //single table
            Schema S = Catalog.GetSchema(Tables.get(0));
            if (S == null) throw new Exception("Table " + Tables.get(0) + " does not exist.");
            // keep only values in requested columns
            S.DisplayTableSomeCols(Columns);
        } else if (!All && Tables.size() >= 2) { //multiple tables
            Schema combindSchema = Catalog.GetSchema(Tables.get(0));
            if (combindSchema == null) throw new Exception("Table " + Tables.get(0) + " does not exist.");
            for(int idx = 1; idx < Tables.size(); idx++) {
                Schema sx = Catalog.GetSchema(Tables.get(idx));
                if (sx == null) throw new Exception("Table " + Tables.get(idx) + " does not exist.");
                combindSchema = combindSchema.cartesianJoin(combindSchema, sx);
            }
            combindSchema.DisplayTableSomeCols(Columns);
        }

        return ++Index;
    }

    private static final TokenType[] Literals = {INT_LITERAL, DOUBLE_LITERAL, STRING_LITERAL, TRUE, FALSE, NULL};
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

        // WHERE <condition>
        if(Input[Index].Type == WHERE){
            WhereResult WhereRS = Where(++Index, Input, Tables);
            WhereClassInterface WhereTree = WhereRS.WhereNode;
            System.out.println(WhereTree.print());
            Index = WhereRS.Index;
            // Semicolon
            Validate(Input[Index], SEMICOLON);
            // TODO Make new table using schema of old table
            Schema S = Catalog.GetSchema(Name).Copy();
            // TODO Insert into new table rows where WHERE == FALSE
            // TODO Delete old table
        }
        else{
            // Semicolon
            Validate(Input[Index], SEMICOLON);
            
            // TODO Delete all entries in table
        }

        return ++Index;
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
            PLUS, MINUS, MULT, DIV, IS, NOT
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

    private static WhereResult Where(int Index, Token[] Input, ArrayList<String> table) throws Exception{
        Deque<InterfaceOperandNode> vals = new ArrayDeque<>();
        Deque<Token> ops = new ArrayDeque<>();
        Deque<WhereClassInterface> whereTreeNodes = new ArrayDeque<>();


        while(Input[Index].Type != SEMICOLON && Input[Index].Type != ORDERBY){
            Token T = Input[Index++];
            // Handling if token is AND/OR
            if(T.Type == AND || T.Type == OR){
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
                        //Check if the attribute exists in the table
                        if(Schema.getAttribute(T.Literal, Catalog.GetSchema(table.get(0))) == null){
                            throw new Exception("Attribute " + T.Literal + " does not exist in table: " + table.get(0));
                        }
                        vals.push(new AttributeValueNode(table.get(0),T.Literal));
                    }
                    else if(next.Type == PERIOD){

                        //Check if the table is valid
                        if(!(table.contains(T.Literal))){
                            throw new Exception("Table: " + T.Literal + " is not a valid table. Provided Tables: " + table);
                        }

                        Index++;
                        Token attrName = Input[Index++];
                        //Check if the attribute exists in the table
                        if(Schema.getAttribute(attrName.Literal, Catalog.GetSchema(T.Literal)) == null){
                            throw new Exception("Attribute " + attrName.Literal + " does not exist in table: " + T.Literal);
                        }
                        if(attrName.Type != NAME_LITERAL){
                            throw new Exception("Unexpected tokens: " + T.Type + ", " + T.Type + ", " + attrName.Type.toString() + " | Expected tokens: NAME_LITERAL, PERIOD, NAME_LITERAL");
                        }

                        //Create AttributeValueNode given Schema and Column and push it into vals
                        AttributeValueNode attributeval = new AttributeValueNode(T.Literal, attrName.Literal);
                        vals.push(attributeval);

                    }
                    else throw new Exception("Unexpected tokens: " + T.Type + ", " + next.Type.toString() + " | Expected tokens: NAME_LITERAL, PERIOD or Operator");
                }
                else if(PossibleVals.contains(T.Type)) vals.push(new ConstantValueNode(T.Literal, T.Type));
                else throw new Exception("Unexpected token: " + T.Type.toString() + ", expected literal value");
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
            if(PossibleOps.contains(op.Type)){
                InterfaceOperandNode right = vals.pop();
                InterfaceOperandNode left = vals.pop();
                whereTreeNodes.push(new BinaryOpNode(left, op.Type, right));
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
