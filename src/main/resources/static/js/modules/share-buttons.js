const initShareContainer = (container) => {
    const facebookLink = container.querySelector("[data-share='facebook']");
    const zaloLink = container.querySelector("[data-share='zalo']");
    const copyBtn = container.querySelector("[data-share='copy']");

    const pageUrl = window.location.href;
    const shareTitle = container.dataset.shareTitle || document.title || "";

    if (facebookLink) {
        const facebookUrl = `https://www.facebook.com/sharer/sharer.php?u=${encodeURIComponent(pageUrl)}`;
        facebookLink.setAttribute("href", facebookUrl);
    }

    if (zaloLink) {
        const zaloUrl = `https://zalo.me/share?url=${encodeURIComponent(pageUrl)}&title=${encodeURIComponent(shareTitle)}`;
        zaloLink.setAttribute("href", zaloUrl);
    }

    if (copyBtn) {
        copyBtn.addEventListener("click", async () => {
            try {
                await navigator.clipboard.writeText(pageUrl);
                const originalText = copyBtn.textContent;
                copyBtn.textContent = "Đã sao chép";
                setTimeout(() => {
                    copyBtn.textContent = originalText;
                }, 1200);
            } catch (error) {
                console.error("Không thể sao chép liên kết:", error);
                alert("Không thể sao chép liên kết. Vui lòng thử lại.");
            }
        });
    }
};

document.addEventListener("DOMContentLoaded", () => {
    document.querySelectorAll(".js-share-container").forEach((container) => initShareContainer(container));
});
