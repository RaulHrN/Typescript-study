 interface LojaProps{
    nome: string;
    endereco: string;
    status: boolean;
 }

const BurguerK: LojaProps = {
    nome: "BurguerK",
    endereco: "Rua Avenida Alameda",
    status: false
}

console.log(BurguerK);

function novaLoja({nome, endereco, status}: LojaProps): void {
    console.log(`Loja ${nome} criada com sucesso!`);
    console.log(`Endereço ${endereco} criado com sucesso!`);
    console.log(`Status: ${status}`);
}

novaLoja({nome: "Helena Gym", endereco: "Avenida sem saida", status: true});