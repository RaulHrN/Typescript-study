"use strict";
//Type string
let tecnologia;
tecnologia = "Javascript ";
console.log("String Type: " + tecnologia);
//Type number = int, float, hex, binary
let valor = 5;
console.log("Number Type: " + valor);
//Union type = permite a tipagem ser mais flexivel
let userId;
userId = "Raul";
console.log("Union Type - Teste 1: " + userId);
userId = 123;
console.log("Union Type - Teste 2: " + userId);
//Arrays
let notes;
notes = ["raul", "fernando", "bruno"];
console.log("Array Type - Teste 1: " + notes);
let caixas; //Array + Union type
caixas = ["Raul", 123, 654, "Fernando"];
console.log("Array Type - Teste 2: " + caixas);
//Tupla (só existe no TS)
let aluno;
aluno = ["Raul", 22];
console.log("Tupla Type: " + aluno);
//Object
let novoUsuario;
novoUsuario = {
    nome: "Raul",
    email: "raul@mail.com",
    idade: 22
};
console.log("Object Type: " + novoUsuario);
//ENUM
var DesignColors;
(function (DesignColors) {
    DesignColors["white"] = "#FFFFFF";
    DesignColors["black"] = "#000000";
})(DesignColors || (DesignColors = {}));
console.log("Enum Type - Test 1: " + DesignColors.black);
var StatusPermission;
(function (StatusPermission) {
    StatusPermission[StatusPermission["ADMIN"] = 0] = "ADMIN";
    StatusPermission[StatusPermission["USER"] = 1] = "USER";
    StatusPermission[StatusPermission["SUPPORT"] = 2] = "SUPPORT";
})(StatusPermission || (StatusPermission = {}));
console.log("Enum Type - Test 1: " + StatusPermission.USER);
//NULL
let nome2;
