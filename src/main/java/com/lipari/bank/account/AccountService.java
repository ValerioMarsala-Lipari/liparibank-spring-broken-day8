package com.lipari.bank.account;

import com.lipari.bank.account.dto.AccountRequest;
import com.lipari.bank.account.model.Account;
import com.lipari.bank.shared.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class AccountService {

    private final AccountRepository accountRepository;

    public List<Account> findAll() {
        return accountRepository.findAll();
    }

    public Account findById(Long id) {
        log.debug("Ricerca conto con id: {}", id);
        return accountRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Account", id));
    }

    @Transactional
    public Account create(AccountRequest request) {
        log.info("Creazione conto: IBAN={}", request.iban());
        Account account = new Account(
                null,
                request.iban(),
                request.balance(),
                request.ownerName()
        );
        return accountRepository.save(account);
    }

    @Transactional
    public void delete(Long id) {
        Account account = findById(id);
        log.info("Eliminazione conto: id={}, IBAN={}", id, account.getIban());
        accountRepository.delete(account);
    }
}
