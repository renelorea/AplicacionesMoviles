<?php
require 'db.php';

try {
    $stmt = $pdo->query("SELECT id, usuario, nombre_completo, correo FROM usuarios");
    $usuarios = $stmt->fetchAll();
    echo json_encode(["success" => true, "usuarios" => $usuarios]);
} catch (Exception $e) {
    http_response_code(500);
    echo json_encode(["success" => false, "msg" => $e->getMessage()]);
}
?>
