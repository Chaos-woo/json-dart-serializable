# json-dart-serializable
A simple tool for converting JSON string to Dart class. Also, the most important thing is that want to adapt the [json_serializable](https://pub.dev/packages/json_serializable) library for developing Flutter application.

# Environment
* IntelliJ IDEA
* Android Studio

# Jetbrains plugins marketplace
* [plugin guide](https://plugins.jetbrains.com/plugin/21392-json-dart-serializable)

# Extension JSON syntax for generated
```json
{
	"basis": "@bext;@name:basic;@type:int/double/string/datetime/bool;@val:default value;@remark:Remark",
	"object": {
		"@oext": "@name:Object;@remark:Remark",
		"other_int": 1,
		"other_string": "string"
	},
	"basis_array": [
		"@arrbext;@name:1;@type:int/double/string/datetime/bool;@remark:Remark"
	],
	"object_array": [
		{
			"@arroext": "@name:ArrayOject;@remark:Remark",
			"other_bool": true,
			"other_double": 3.14
		}
	]
}
```
* `@bext`, `@oext`, `@arrbext`, `@arroext` indicates the syntax and has no special meaning.