# json-dart-serializable
A simple tool for converting JSON string to Dart class. Also, 
the most important thing is that want to adapt the [json_serializable](https://pub.dev/packages/json_serializable) library 
for developing Flutter application.

# Environment
* IntelliJ IDEA
* Android Studio

# Jetbrains plugins marketplace
* [plugin guide](https://plugins.jetbrains.com/plugin/21392-json-dart-serializable)

# Characteristic
Quickly generate a tree of model nodes through Json and view the Dart data objects to be generated in Json tables.

# Extension JSON syntax for generated
An extended implementation that allows field names, data types, 
default values, nullability, and comments on **Original JSON** to reduce repeated modifications on Json tables(although, 
I designed this Json table for easy viewing and modification, haha...). 
Therefore, after one modification, the next time can continue to maintain the previous modification to continue to modify.
Sure, Json tables will be modified by extended Json syntax...
```json
{
	"basis": "@bext;@name:basic;@type:int/double/string/datetime/bool;@val:default value;@nullable;@remark:Remark",
	"object": {
		"@oext": "@oext;@name:Object;@nullable;@remark:Remark",
		"other_int": 1,
		"other_string": "string"
	},
	"basis_array": [
		"@arrbext;@name:1;@type:int/double/string/datetime/bool;@nullable;@remark:Remark"
	],
	"object_array": [
		{
			"@arroext": "@arroext;@name:ArrayOject;@nullable;@remark:Remark",
			"other_bool": true,
			"other_double": 3.14
		}
	]
}
```
* `@bext`, `@oext`, `@arrbext`, `@arroext` only indicates the syntax and has no special meaning.
* Extension mode default is opened, it will be used with normal parse mode. Generally, you can use the extension mode enabled by default.