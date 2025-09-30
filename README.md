# GenZ Language (Revising) 

## Creator

Michaela Borces and Marinelle Joan Tambolero

## Language Overview

GenZ Language is a programming language designed to blend coding with GenZ internet slang, memes, and emojis. Its purpose is both educational and comedic: to demonstrate how syntax and semantics can be remixed into something fun and relatable while still keeping the structure of a real programming language.  

Main Characteristics: 

Slang Keywords - Instead of usual programming words, GenZ Lang uses slang. For example, vibe is used to declare variables, spill is for printing output,  slay is for returning output, and bet and deadass are used for if-else conditions. This makes the code look casual and more like how GenZ talks online. 

Literal Values with Slang - The language supports numbers, strings, characters, booleans, and null values. Booleans are written as fr (true) and cap (false), while ghosted is used for null.

Easy to Read ( if familiar with Slang) - Someone who understands GenZ slang will find the code easy to “read” because it feels like normal conversation, just with programming structure added. 

Humor-Oriented Design - The language is intentionally humorous and informal. Its error messages and keywords are phrased in slang to reflect conversational styles rather than technical jargon. 

## Keywords

"bet" for if → used to run code only when a condition is true

"deadass" for else → runs if the “if” condition fails

"lowkey" for for → loop that repeats code with iteration

"cap" for false → represents the Boolean value false

"fr" for true → represents the Boolean value true

"tea" for var → used to declare a variable

"ghosted" for null → means no value / nothing

"summon" for fun → defines a function

"spill" for print → outputs text or values

"slay" for return → exits a function and gives back a value

"super" for super → calls a method/constructor from the superclass

"dis" for this → refers to the current object

"highkey" for while → loop that runs while a condition is true

"squad" for class → defines a class

"cancelledt" for break → exits a loop immediately

"yeet" for continue → skips to the next loop iteration

"pullup" for import → brings in external code or modules

Operators

Arithmetic 

  - Addition (+)
  
  - Subtraction (-)
  
  - Multiplication (*)
  
  - Division (/) 
  
  - Modulo (%) 
  
  - Exponent (^) 
  
  - Increment (++)
  
  - Decrement (--) 
  
  - Comparison 
  
  - Equal (==)
  
  - Not Equal (!=) 
  
  - Greater Than (>)
  
  - Less Than (<) 
  
  - Greater Than or Equal to (>=)
  
  - Less Than or Equal to (<=) 

Logical  
  
  - AND (&)
  
  - OR    (|)
  
  - NOT  (!) 

Assignment 
  
  - Assign (=) 

## Literals

Numbers 

  - Standard integers and decimals 

Strings 

  - Double quotes (“ “) wrap text. 

Characters 

  - Single quotes (‘ ‘) wrap a single character. 

Boolean Values 
    
  - Fr (TRUE) 

  - Cap (FALSE)
    
Null/None 
    
  - Ghosted (null/undefined) 

## Identifiers

Rules for Valid Identifiers (Same for all category): 
- Not case-sensitive
- Should not start with numbers 
- _  is only the symbol allowed to start an identifier 

## Comments

- In starting a comment the syntax should be ‘FYI.’.  
- Nested comments are not supported. 

## Syntax Style

- White space is not significant, new lines are ignored
- Keywords are case sensitive (must follow lower case syntax)
- No delimiter 

## Sample Code

spill "Hey bestie, it's giving Hello World vibes"

tea mood = "vibing"
tea age = 21

tea vibeCheck = fr

bet (vibeCheck == fr) {  
    spill "You passed the vibe check"
} deadass {  
    spill "No vibe detected"
}  

lowkey (tea i = 0; i < 3; i++) {  
    spill "This is loop #" + i
}  

summon intro(tea name) {  
    slay "Slay queen, I’m  " + name
}  
spill intro("Michaela")

## Design Rationale

  GenZ Lang was created to make coding more approachable and less intimidating, especially for younger audiences who are familiar with online slang and humor. By blending internet culture with programming concepts, the language lowers the barrier to entry while still maintaining the logical structure of traditional programming.

  The use of slang-based keywords gives beginners a sense of familiarity, as the terms mirror how they already communicate online. At the same time, experienced coders can still recognize the parallels to established programming languages, making it both humorous and relatable. This dual design ensures that while the language feels casual and fun, it doesn’t lose its connection to real-world programming principles.


