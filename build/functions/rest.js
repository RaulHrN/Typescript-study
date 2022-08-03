"use strict";
/* function totalVendas(venda1: number, venda2: number, venda3: number, venda4: number): number{
    const total = venda1 + venda2 + venda3 + venda4;
    console.log(total);
    return total;
} */
function totalVendas(...vendas) {
    const qtdVendas = vendas.length;
    console.log(`Você fez ${qtdVendas} vendas hoje`);
    vendas.map(venda => {
        console.log(`R$ ${venda}`);
    });
}
totalVendas(34, 34, 12, 1, 434, 6, 63, 6, 3, 56, 4, 34, 5);
