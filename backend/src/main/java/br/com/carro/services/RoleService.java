package br.com.carro.services;

import br.com.carro.entities.Role.Role;
import br.com.carro.repositories.RoleRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

@Service
public class RoleService {
    @Autowired
    private final RoleRepository roleRepository;

    public RoleService(RoleRepository roleRepository) {
        this.roleRepository = roleRepository;
    }

    public Role cadastrar(Role role) {
        return roleRepository.save(role);
    }

    public String atualizar(Long id, Role role) {
        role.setId(id);
        this.roleRepository.save(role);
        return "Role atualizado com sucesso!";
    }
    public String excluir(Long id) {
        roleRepository.deleteById(id);
        return "Role excluído com sucesso!";
    }

    public Role buscarPorId(Long id) {
        return roleRepository.findById(id).orElse(null);
    }

    public Page<Role> listar(Pageable pageable) {
        return roleRepository.findAll(pageable);
    }
}
