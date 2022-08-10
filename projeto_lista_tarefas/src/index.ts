let listElement = document.querySelector("#app ul") as HTMLUListElement;
let inputElement = document.querySelector("#app input") as HTMLInputElement;
let buttonElement = document.querySelector("#app button") as HTMLButtonElement;

let listaSalva: (string | null) = localStorage.getItem("@listagem_tarefas");
let tarefas: string[] = listaSalva !== null && JSON.parse(listaSalva) || [];

function listarTarefas(){
    listElement.innerHTML = "" ;

    tarefas.map(item => {
        let todoElement = document.createElement("li");
        let tarefaText = document.createTextNode(item);

        let linkElement = document.createElement("a");
        linkElement.setAttribute("href", "#");

        let posicao = tarefas.indexOf(item);

        linkElement.setAttribute("onclick", `deletarTarefa(${posicao})`)
        
        let linkText = document.createTextNode("Excluir");
        linkElement.appendChild(linkText);

        todoElement.appendChild(tarefaText);
        todoElement.appendChild(linkElement);
        todoElement.appendChild(tarefaText);
    })
}