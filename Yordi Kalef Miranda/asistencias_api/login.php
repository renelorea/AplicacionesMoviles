<?php
require 'db.php';
$data = json_decode(file_get_contents('php://input'), true);

if (!$data || !isset($data['usuario']) || !isset($data['contrasena'])) {
    http_response_code(400);
    echo json_encode(["success" => false, "msg" => "Datos incompletos"]);
    exit;
}

$usuario = $data['usuario'];
$contrasena = $data['contrasena'];

$stmt = $pdo->prepare("SELECT * FROM usuarios WHERE usuario = :usuario LIMIT 1");
$stmt->execute(['usuario' => $usuario]);
$user = $stmt->fetch();

if ($user && password_verify($contrasena, $user['contrasena'])) {
    unset($user['contrasena']);
    echo json_encode(["success" => true, "usuario" => $user]);
} else {
    echo json_encode(["success" => false, "msg" => "Usuario o contraseña incorrectos"]);
}
?>

