# Chat Lang

## Creator

Michaela Borces (mika-29)
Marinelle Joan Tambolero (NellePot)

## Language Overview

Our programming language is designed to function like a chatbot-style program, allowing users to interact through simple commands and responses. It utilizes easy-to-understand keywords and a natural language feel, making code writing feel more like giving instructions in plain English.
The language focuses on user input, displaying messages, loops, conditionals, and functions — the basic building blocks of most programs. It also uses special symbols to show variable types and simple syntax rules to keep things easy for beginners.
Overall, this language is meant to help users learn programming concepts in a fun and conversational way, while also being capable of creating simple chatbot-like or interactive programs.


Main Characteristics: 

Straight to the point - Instead of traditional programming terms like print, string, boolean, or function, [Lang Name] uses clear, everyday words that sound more natural to beginners. This makes the code feel less like technical programming and more like giving direct instructions. Commands are short, readable, and focused only on what the user wants to happen, removing unnecessary symbols or complex syntax. Because the language avoids clutter and uses plain English phrases, even first-time programmers can quickly understand what each line does without needing to memorize complicated keywords.

Minimal Symbols - It limits the use of punctuation like "{}", ";", or complicated operators. Only a few readable symbols (like $ for variables or . for delimeters) are used to avoid confusing beginners.

## Keywords

- "if" → used to run code only when a condition is true
- "then" → used for nested ifs
- "otherwise" → runs if the “if” condition fails
- "done if" → ends the if statement
- "repeat this n times" → loop that repeats code with iteration
- "repeat this until n" → loop that repeats until the condition is met 
- "listen" → for user input 
- "as" → for assignment 
- "false" → represents the Boolean value false
- "true" → represents the Boolean value true
- "create a function “name” " → defines a function
- "say" → outputs/prints text or values
- "return" → exits a function and gives back a value
- "done" → ends a function 


## Operators

Arithmetic 

  - Addition (+)
  - Subtraction (-)
  - Multiplication (*)
  - Division (/) 
  - Modulo (%) 
  - Exponent (^) 
  - Increment by 1 (--)
  - Decrement by 1 (++)
    
Comparison 
  
  - Equal (==)
  - Not Equal (!=) 
  - Greater Than (>)
  - Less Than (<) 
  - Greater Than or Equal to (>=)
  - Less Than or Equal to (<=) 
  
Logical  
  
  - AND (&)
  - OR   (|)
  - NOT  (!) 

## Literals

Numbers 

  - Standard integers and decimals 

Strings 

  - Double quotes (“ “) wrap text. 

Characters 

  - Single quotes (‘ ‘) wrap a single character. 

Boolean Values 

  Keywords:
  - True
  - False
    

## Identifiers

Rules for Valid Identifiers (Same for all category): 
- Not case-sensitive
- Should not start with numbers 
- Must begin with a specific symbol depending on the data type: 
    @ - String 
    $ - Int 
    % - Float 

## Comments

- In starting a comment the syntax should be ‘FYI.’.  
- Nested comments are not supported.
- Example: FYI. This is a comment 

## Syntax Style

- White space is not significant, new lines are ignored
- Keywords are case sensitive (must follow lower case syntax)
- The colon (:) is used as a block delimiter to indicate the start of a control structure, function, or conditional statement block.
- The colon (:) is used as a block delimiter to indicate the end of each line
- No indentation rules

## Sample Code

# User Input:

  listen “What’s your name?” as @name.
  $value as 3.
  say @name.
  say $value. 

# Loops:

  $n as 10.
  repeat this n times:
  $value as $value + 3 .
  stop when:
  $value is 16.

# Function creation:

  create a function “greet” :
  say "Hello!".
  return.
  done.

  create a function “addNumbers” with parameters -> $a, $b:
  say $a + $b.
  return.
  done.


  use (functionName).

## Design Rationale

  We designed our programming language to be simple, readable, and beginner-friendly, especially for users who are new to coding. Our main goal was to make programming feel more like talking to a chatbot — using natural language and intuitive commands instead of complex syntax.
  We chose keywords such as “Listen”, “Say”, and “Repeat this n times” to make the code easy to understand, even at first glance. These commands resemble everyday language, helping users quickly grasp how input, output, loops, and conditions work.
Overall, our design decisions focused on making the language feel conversational and logical, while still teaching the core principles of programming like flow control, variable usage, and functions in a fun and approachable way.



