//Type string
let tecnologia: string;
tecnologia = "Javascript ";
console.log("String Type: " + tecnologia);

//Type number = int, float, hex, binary
let valor: number = 5;
console.log("Number Type: " + valor);
//Union type = permite a tipagem ser mais flexivel
let userId: number | string;
userId = "Raul";
console.log("Union Type - Teste 1: " + userId);
userId = 123;
console.log("Union Type - Teste 2: " + userId);

//Arrays
let notes: string[];
notes = ["raul", "fernando", "bruno"];
console.log("Array Type - Teste 1: " + notes);
let caixas: (string | number)[]; //Array + Union type
caixas = ["Raul", 123, 654, "Fernando"]
console.log("Array Type - Teste 2: " + caixas);

//Tupla (só existe no TS)
let aluno: [string, number];
aluno = ["Raul", 22];
console.log("Tupla Type: " + aluno);

//Object
let novoUsuario: Object;
novoUsuario = {
    nome: "Raul",
    email: "raul@mail.com",
    idade: 22
}
console.log("Object Type: " + novoUsuario);

//ENUM
enum DesignColors {
    white = "#FFFFFF",
    black = "#000000"
}
console.log("Enum Type - Test 1: " + DesignColors.black);

enum StatusPermission{
    ADMIN,
    USER,
    SUPPORT
}
console.log("Enum Type - Test 1: " + StatusPermission.USER);

//NULL
let nome2: null;