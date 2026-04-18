package com.example.marketplace.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;

import com.example.marketplace.model.ItemCarrinho;
import com.example.marketplace.model.Produto;
import com.example.marketplace.model.ResumoCarrinho;
import com.example.marketplace.model.SelecaoCarrinho;
import com.example.marketplace.repository.ProdutoRepository;

@Service
public class ServicoCarrinho {

    private final ProdutoRepository repositorioProdutos;

    public ServicoCarrinho(ProdutoRepository repositorioProdutos) {
        this.repositorioProdutos = repositorioProdutos;
    }

    public ResumoCarrinho construirResumo(List<SelecaoCarrinho> selecoes) {

        List<ItemCarrinho> itens = new ArrayList<>();

        // =========================
        // Monta os itens do carrinho
        // =========================
        for (SelecaoCarrinho selecao : selecoes) {
            Produto produto = repositorioProdutos.buscarPorId(selecao.getProdutoId())
                    .orElseThrow(
                            () -> new IllegalArgumentException("Produto não encontrado: " + selecao.getProdutoId()));

            itens.add(new ItemCarrinho(produto, selecao.getQuantidade()));
        }

        int quantidadeDeItems = selecoes.stream().mapToInt(SelecaoCarrinho::getQuantidade).sum();
        int descontoPorQuantidadeDeItem = 0;
        switch (quantidadeDeItems) {
            case 2 -> {
                descontoPorQuantidadeDeItem = 5;
                break;
            }
            case 3 -> {
                descontoPorQuantidadeDeItem = 7;
                break;
            }
            case 4 -> {
                descontoPorQuantidadeDeItem =  10;
                break;
            }
            default -> {
                descontoPorQuantidadeDeItem = 0;
            }
        }

        int percentualTotalPorCategoria = 0;
        for(ItemCarrinho item : itens) {
            switch (item.getProduto().getCategoria()) {
                case CAPINHA, FONE -> {
                    percentualTotalPorCategoria += 3;
                }
                case CARREGADOR -> {
                    percentualTotalPorCategoria += 5;
                }
                case PELICULA, SUPORTE -> {
                    percentualTotalPorCategoria += 2;
                }
            }
        }

        BigDecimal percentualDesconto = BigDecimal.valueOf(percentualTotalPorCategoria + descontoPorQuantidadeDeItem);
        if(percentualDesconto.intValue() > 25) {
            percentualDesconto = BigDecimal.valueOf(25);
        }

        // =========================
        // Calcula subtotal
        // =========================
        BigDecimal subtotal = itens.stream()
                .map(ItemCarrinho::calcularSubtotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);


        BigDecimal valorDesconto = percentualDesconto.divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);;
        valorDesconto = subtotal.multiply(valorDesconto);

        BigDecimal total = subtotal.subtract(valorDesconto);


        return new ResumoCarrinho(itens, subtotal, percentualDesconto, valorDesconto, total);
    }
}
