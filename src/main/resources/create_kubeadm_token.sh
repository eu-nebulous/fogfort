token=$(kubeadm token generate)
kubeadm token create ${token} --print-join-command --ttl=1h
echo "!!NEB_SCRIPT_RESULT_KUBERNETES_JOIN_TOKEN:${token}!!"