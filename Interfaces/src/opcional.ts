interface CadastroProps{
    nome?: string;
    email: string;
    status: boolean;
}

function novoCadastro (propriedades: CadastroProps): void {
    console.log(propriedades.nome);
}

novoCadastro ({email: "raul@mail.com", status: true});