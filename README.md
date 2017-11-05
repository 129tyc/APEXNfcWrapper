# APEXNfcWrapper
A android library packs APEX Nfc library

# Subject
The library designed for using APEX Nfc library easier.

# Instruction
- Extend `NfcActivity` and implement abstract method `onSetNfcWrapListener`.
- Implement `NfcWrapListener`and write your code.
- Get a `NfcCommander` instance from method `setNfcCommander` in interface `NfcWrapListener`.
- return a `NfcWrapListener` instance in method `onSetNfcWrapListener`.
- Juse use it!

**Anything else can reference java comments and sample app.**
