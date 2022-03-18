# Compendium-App
Android Companion Device App

## Protocol Design
The protocol framework is similar to that of the python library but with an additional iteration. In much the same way there are a series of subclass and interfaces that define particular behaviour. Java Reflection is then used to determine what functions to call on a generic message object. Unlike the Python equivalent this aims to minimise the per message logic necessary, in particular with regards to actions like loading and saving data to and from messages which is now defined as static fields.

### Protocol Data
Unlike the Python implementation in which data is stored in field specific variables within a Protocol subclass this implementation uses a generic Protocol Data Map<String,String>. This is passed to messages during processing so they may read and write to it as a shared store between different messages. It can also be updated by external processes, for example, data that has been received from the UI or crypto processing that has taken place outside of the protocol stack. 

### Updates from the UI
A protocol is inherently designed to run to completion once it starts, for example, when it receives a message it will attempt to construct and send the next message immediately. Whenever that is not suitable, for example, if the protocol needs the user provide biometric authentication to access a key and perform some action on supplied protocol data the protocol enters a `AWAITING_UI` state. In such a state the protocol will sit idle until either a valid message is received via the web socket  - which should be impossible since it will be in a state requiring a response message which only the Companion Device should be sending - or it receives the updateFromUI call with additional data. At that point it will continue processing the next message. As such, it is essential that the updateFromUI call is only made after all necessary data has been added to the Protocol Data object.