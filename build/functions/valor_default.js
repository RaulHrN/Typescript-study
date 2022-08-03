"use strict";
function cadastro(email, senha, nome = "Aluno", idade) {
    let data = { email, senha, nome, idade };
    console.log(data);
}
cadastro("teste@teste.com", "123123", "Raul", 124);
