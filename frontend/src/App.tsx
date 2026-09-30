import { createBrowserRouter, RouterProvider } from "react-router-dom";
import { Layout } from "./components/Layout";
import { DashboardPage } from "./pages/DashboardPage";
import { LeaderboardPage } from "./pages/LeaderboardPage";
import { ProblemPage } from "./pages/ProblemPage";
import { ProblemsPage } from "./pages/ProblemsPage";
import { ScratchpadPage } from "./pages/ScratchpadPage";

const router = createBrowserRouter([
  {
    element: <Layout />,
    children: [
      { path: "/", element: <ProblemsPage /> },
      { path: "/problems/:slug", element: <ProblemPage /> },
      { path: "/scratchpad", element: <ScratchpadPage /> },
      { path: "/me", element: <DashboardPage /> },
      { path: "/leaderboard", element: <LeaderboardPage /> },
    ],
  },
]);

export function App() {
  return <RouterProvider router={router} />;
}
