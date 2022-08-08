interface TecnologiaProps{
    id: string;
    nome: string;
    descricao?: string;
}

interface NomesProps{
    tecnologia: TecnologiaProps[];
}

let frontend: NomesProps = {
    tecnologia: [
        {id: "12", nome: "ReactJS", descricao: "Biblioteca para criar interfaces"},
        {id: "43", nome: "VueJS", descricao: "Framework FrontEnd"}
    ]
}